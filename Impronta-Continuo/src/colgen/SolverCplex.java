package colgen;

import java.util.ArrayList;
import java.util.Set;

import com.vividsolutions.jts.geom.Point;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import impronta.Instancia;
import impronta.Pad;
import impronta.Solucion;

public class SolverCplex
{
	private Instancia _instancia;
	private ArrayList<Pad> _pads;
	private ArrayList<Point> _points;
	private IloCplex _cplex;
	private ArrayList<IloNumVar> _variables;
	
	public SolverCplex(Instancia instancia, Set<Pad> pads, Set<Point> points)
	{
		_instancia = instancia;
		_pads = new ArrayList<Pad>(pads);
		_points = new ArrayList<Point>(points);
	}
	
	public Solucion resolver()
	{
		construirModelo();
		return obtenerSolucion();
	}
	
	private void construirModelo()
	{
		try
		{
			_cplex = new IloCplex();
			_variables = new ArrayList<IloNumVar>();
			
			for(int i=0; i<_pads.size(); ++i)
				_variables.add(_cplex.boolVar());
			
			IloNumExpr objetivo = _cplex.linearIntExpr();
			for(int i=0; i<_pads.size(); ++i)
				objetivo = _cplex.sum(objetivo, _cplex.prod(objetivo(_pads.get(i)), _variables.get(i)));
			
			_cplex.addMaximize(objetivo);
			
			for(Point point: _points)
			{
				IloNumExpr lhs = _cplex.linearNumExpr();
				for(int i=0; i<_pads.size(); ++i) if( _pads.get(i).contiene(point) )
					lhs = _cplex.sum(lhs, _variables.get(i));
				
				_cplex.addLe(lhs, 1);
			}
		}
		catch(IloException e)
		{
			e.printStackTrace();
		}
	}
	
	private Solucion obtenerSolucion()
	{
		Solucion ret = new Solucion(_instancia);
		
		try
		{
			if( _cplex.solve() )
			{
				for(int i=0; i<_pads.size(); ++i) if( _cplex.getValue(_variables.get(i)) > 0.9 )
					ret.agregarPad(_pads.get(i));
			}
		}
		catch(IloException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	
	// Coeficiente de la funcion objetivo asociado con cada pad
	private double objetivo(Pad pad)
	{
		return pad.getArea();
	}
}
