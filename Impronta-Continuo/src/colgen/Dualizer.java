package colgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Point;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import impronta.Pad;

public class Dualizer
{
	private SolverCG _solverCG;
	private Map<Pad, Double> _primales;
	private Map<Point, Double> _duales;
	
	private ArrayList<Point> _variables;
	private ArrayList<Pad> _restricciones;

	private IloCplex _cplex;
	private Map<Pad, IloNumVar> _vvars;
	private Map<Point, IloNumVar> _yvars;

	public Dualizer(SolverCG solver)
	{
		_solverCG = solver;
		_primales = solver.primales();
		_duales = solver.duales();
	}

	// Resuelve el modelo
	public boolean resolver()
	{
		boolean ret = false;

		try
		{
			ret = resolverUnsafe();
			
//			for(Point punto: _variables)
//				System.out.println("   y(" + punto + ") = " + _cplex.getValue(_yvars.get(punto)));
//			
//			System.out.println("   Dualizer objective = " + _cplex.getObjValue());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}

	public boolean resolverUnsafe() throws IloException
	{
		_variables = new ArrayList<Point>(_duales.keySet());
		_restricciones = new ArrayList<Pad>(_primales.keySet());

		_cplex = new IloCplex();
		_cplex.setOut(null);
		
		_vvars = new HashMap<Pad, IloNumVar>();
		_yvars = new HashMap<Point, IloNumVar>();
		
		for(Point punto: _variables)
			_yvars.put(punto, _cplex.numVar(0, Double.MAX_VALUE));
		
		for(Pad pad: _restricciones)
			_vvars.put(pad, _cplex.numVar(0, Double.MAX_VALUE));

		IloNumExpr objetivo = _cplex.linearIntExpr();
		for(Pad pad: _restricciones)
			objetivo = _cplex.sum(objetivo, _vvars.get(pad));
		
		_cplex.addMinimize(objetivo);
		
		for(Pad pad: _restricciones)
		{
			IloNumExpr lhs = _cplex.linearNumExpr();
			for(Point punto: _variables) if( pad.contiene(punto) )
				lhs = _cplex.sum(lhs, _yvars.get(punto));
			
			lhs = _cplex.sum(lhs, _cplex.prod(-1, _vvars.get(pad)));
			_cplex.addGe(lhs, SolverCG.objetivo(pad));
		}
		
		IloNumExpr ths = _cplex.linearNumExpr();
		for(Point punto: _variables)
			ths = _cplex.sum(ths, _yvars.get(punto));
		
		_cplex.addEq(ths, _solverCG.getCplex().funcionObjetivo());
		System.out.println("Dualizer target: " + _solverCG.getCplex().funcionObjetivo());

		return _cplex.solve();
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
		return _restricciones;
	}
	public ArrayList<Point> getPuntos()
	{
		return _variables;
	}
	public Map<Point, Double> getCubrimiento()
	{
		Map<Point, Double> ret = new HashMap<Point, Double>();
		
		try
		{
			for(Point punto: _variables)
				ret.put(punto, _cplex.getValue(_yvars.get(punto)));
		}
		catch(IloException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
}
