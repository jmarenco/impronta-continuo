package colgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import impronta.Pad;

public class Dualizer
{
	private SolverCG _solverCG;
	private Map<Pad, Double> _primales;
	private Map<Point, Double> _duales;
	private ArrayList<Pad> _todos;
	
	private ArrayList<Point> _puntos;
	private ArrayList<Pad> _pads;

	private IloCplex _cplex;
	private Map<Pad, IloNumVar> _vvars;
	private Map<Point, IloNumVar> _yvars;
	
	private Random _random;
	private Pad _padNoFactible;

	public Dualizer(SolverCG solver, Random random)
	{
		_solverCG = solver;
		_random = random;
		_primales = solver.primales();
		_duales = solver.duales();
		_todos = new ArrayList<Pad>(_solverCG.getPads());
	}
	
	public boolean esOptima()
	{
		boolean ret = false;
		
		try
		{
			construirInput();
			if( resolverModelo() == true )
				ret = dualFactible();
		}
		catch(IloException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}

	// Puntos generados hasta ahora, mas algunos puntos adicionales seleccionados aleatoriamente
	private void construirInput()
	{
		construirInputAleatorio();
	}
	private void construirInputAleatorio()
	{
		_pads = new ArrayList<Pad>(_primales.keySet());
		_puntos = new ArrayList<Point>(_duales.keySet());
		
		Collections.shuffle(_todos, _random);
		
		for(int i=0; i<100 && i<_todos.size(); ++i) if( _pads.contains(_todos.get(i)) == false )
			_pads.add(_todos.get(i));
	}
	private void construirInputPorEsquinas()
	{
		_pads = new ArrayList<Pad>(_primales.keySet());
		_puntos = new ArrayList<Point>(_duales.keySet());
		
		Collections.sort(_todos, (x,y) -> esquinasDescubiertas(y) - esquinasDescubiertas(x));

		for(int i=0; i<100 && i<_todos.size(); ++i) if( _pads.contains(_todos.get(i)) == false && _random.nextBoolean() == true )
			_pads.add(_todos.get(i));
	}
	private void construirInputPorArea()
	{
		_pads = new ArrayList<Pad>(_primales.keySet());
		_puntos = new ArrayList<Point>(_duales.keySet());
		
		Map<Pad, Double> c = new HashMap<Pad, Double>();
		Collections.sort(_todos, (x,y) -> (int)Math.signum(areaDescubierta(y,c) - areaDescubierta(x,c)));

		for(int i=0; i<100 && i<_todos.size(); ++i) if( _pads.contains(_todos.get(i)) == false && _random.nextBoolean() == true )
			_pads.add(_todos.get(i));
	}
	private void construirInputTodos()
	{
		_pads = new ArrayList<Pad>(_primales.keySet());
		_puntos = new ArrayList<Point>(_duales.keySet());
		
		for(Pad pad: _todos)
			_pads.add(pad);
	}
	
	// Area de un pad no cubierta por la solucion ni por los pads seleccionados
	private double areaDescubierta(Pad pad, Map<Pad, Double> cache)
	{
		if( cache.containsKey(pad) == false )
		{
			Geometry diferencia = pad.getPerimetro();
			for(Pad otro: _pads)
				diferencia = diferencia.difference(otro.getPerimetro());
		
			cache.put(pad, diferencia.getArea());
		}
		
		return cache.get(pad);
	}
	
	// Esquinas de un pad no cubiertas por la solucion
	private int esquinasDescubiertas(Pad pad)
	{
		int ret = 0;
		for(Coordinate coordinate: pad.getEsquinas())
		for(Pad otro: _pads) if( otro.contiene(coordinate) == false )
			++ret;
		
		return ret;
	}

	private boolean resolverModelo() throws IloException
	{
		_cplex = new IloCplex();
		_cplex.setOut(null);
		
		_vvars = new HashMap<Pad, IloNumVar>();
		_yvars = new HashMap<Point, IloNumVar>();
		
		for(Pad pad: _pads)
			_vvars.put(pad, _cplex.numVar(0, Double.MAX_VALUE, "v" + _pads.indexOf(pad)));
		
		for(Point punto: _puntos)
			_yvars.put(punto, _cplex.numVar(0, Double.MAX_VALUE, "x" + _puntos.indexOf(punto)));

		IloNumExpr objetivo = _cplex.linearIntExpr();
		for(Pad pad: _pads)
			objetivo = _cplex.sum(objetivo, _vvars.get(pad));
		
		_cplex.addMinimize(objetivo);
		
		for(Pad pad: _pads)
		{
			IloNumExpr lhs = _cplex.linearNumExpr();
			for(Point punto: _puntos) if( pad.contiene(punto) )
				lhs = _cplex.sum(lhs, _yvars.get(punto));
			
			lhs = _cplex.sum(lhs, _vvars.get(pad));
			_cplex.addGe(lhs, _solverCG.objetivo(pad));
		}
		
		IloNumExpr ths = _cplex.linearNumExpr();
		for(Point punto: _puntos)
			ths = _cplex.sum(ths, _yvars.get(punto));
		
		_cplex.addEq(ths, _solverCG.getCplex().funcionObjetivo());
		
		return _cplex.solve();
	}
	
	private boolean dualFactible() throws UnknownObjectException, IloException
	{
		_padNoFactible = null;
		Collections.shuffle(_todos, _random);

		for(Pad pad: _todos)
		{
			double lhs = 0;
			for(Point point: _yvars.keySet()) if( pad.contiene(point) )
				lhs += _cplex.getValue(_yvars.get(point));
			
			if( lhs < 0.99 * _solverCG.objetivo(pad) )
			{
				_padNoFactible = pad;
				return false;
			}
		}
		
		return true;
	}
	
	// Consultas
	public IloCplex.Status getCplexStatus()
	{
		IloCplex.Status ret = IloCplex.Status.Error;
		
		try
		{
			ret = _cplex.getStatus();
		}
		catch(IloException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	public double getObjetivo()
	{
		double ret = Double.MAX_VALUE;
		
		try
		{
			ret = _cplex.getObjValue();
		}
		catch(IloException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	public ArrayList<Pad> getPads()
	{
		return _pads;
	}
	public ArrayList<Point> getPuntos()
	{
		return _puntos;
	}
	public Map<Point, Double> getXVars()
	{
		Map<Point, Double> ret = new HashMap<Point, Double>();
		
		try
		{
			for(Point punto: _puntos)
				ret.put(punto, _cplex.getValue(_yvars.get(punto)));
		}
		catch(IloException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	public Pad getPadNoFactible()
	{
		return _padNoFactible;
	}
}
