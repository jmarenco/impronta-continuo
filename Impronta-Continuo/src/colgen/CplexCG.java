package colgen;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Point;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import impronta.Pad;

public class CplexCG
{
	private IloCplex _cplex;
	private IloObjective _objetivo;
	private Map<Pad, IloNumVar> _variables;
	private Map<Point, IloRange> _restricciones;
	private Map<Pad, Double> _primales;
	private Map<Point, Double> _duales;
	
	public CplexCG()
	{
		try
		{
			_cplex = new IloCplex();
			_cplex.setOut(null);
			
			_objetivo = _cplex.addMaximize(_cplex.linearIntExpr());
			_variables = new HashMap<Pad, IloNumVar>();
			_restricciones = new HashMap<Point, IloRange>();
		}
		catch (IloException e)
		{
			e.printStackTrace();
		}
	}
	
	public void agregarVariable(Pad pad, double objCoef)
	{
		if( _variables.containsKey(pad) )
			return;
		
		try
		{
			IloNumVar variable = _cplex.numVar(0.0, 10.0);
			
			_cplex.add(variable);
			_variables.put(pad, variable);
			_cplex.setLinearCoef(_objetivo, objCoef, variable);

			// Agrega la variable a las restricciones de puntos incluidos en el pad
			for(Point point: _restricciones.keySet()) if( pad.contiene(point) )
				_cplex.setLinearCoef(_restricciones.get(point), 1.0, variable);
		}
		catch (IloException e)
		{
			e.printStackTrace();
		}
	}

	public void agregarRestriccion(Point point)
	{
		if( _restricciones.containsKey(point) )
			return;
		
		try
		{
			IloNumExpr lhs = _cplex.linearNumExpr();
			
			for(Pad pad: _variables.keySet()) if( pad.contiene(point) )
				lhs = _cplex.sum(lhs, _variables.get(pad));
			
			_restricciones.put(point, _cplex.addLe(lhs, 1));
		}
		catch (IloException e)
		{
			e.printStackTrace();
		}
	}
	
	public void resolver()
	{
		try
		{
//			_cplex.exportModel("modelo.lp");
			_cplex.solve();
			
			obtenerPrimales();
			obtenerDuales();
		}
		catch (IloException e)
		{
			e.printStackTrace();
		}
	}
	
	private void obtenerPrimales()
	{
		try
		{
			_primales = new HashMap<Pad, Double>();
			for(Pad pad: _variables.keySet())
				_primales.put(pad, _cplex.getValue(_variables.get(pad)));
		}
		catch (IloException e)
		{
			e.printStackTrace();
		}
	}
	
	private void obtenerDuales()
	{
		try
		{
			_duales = new HashMap<Point, Double>();
			for(Point point: _restricciones.keySet())
				_duales.put(point, _cplex.getDual(_restricciones.get(point)));
		}
		catch (IloException e)
		{
			e.printStackTrace();
		}
	}
	
	public Map<Pad, Double> primales()
	{
		return _primales;
	}
	public Map<Point, Double> duales()
	{
		return _duales;
	}
	public int cantidadVariables()
	{
		return _variables.size();
	}
	public int cantidadRestricciones()
	{
		return _restricciones.size();
	}
	public long variablesFraccionarias()
	{
		return primales().values().stream().filter(i -> 0.01 < i && i < 0.99).count();		
	}
	public double funcionObjetivo()
	{
		double ret = 0;
		
		try
		{
			ret = _cplex.getObjValue();
		}
		catch (IloException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
}
