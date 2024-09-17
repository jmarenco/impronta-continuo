package continuo;

import com.vividsolutions.jts.geom.Coordinate;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import impronta.Instancia;
import impronta.Pad;
import impronta.Solucion;

public class SolverContinuo
{
	private Instancia _instancia;
	private IloCplex _cplex;
	private Modelo _modelo;
	
	private boolean _eliminacionSimetrias = true;
	private boolean _cutPool = true;
	private Modelo.Objetivo _objetivo = Modelo.Objetivo.Cantidad;
	private double _tiempoMaximo;
	private int _pads = 20;
	
	private double _objvalue;
	private double _gap;
	private int _nodes;
	private int _cuts;
	private String _status;
	
	public SolverContinuo(Instancia instancia)
	{
		_instancia = instancia;
	}
	
	public void setEliminacionSimetrias(boolean valor)
	{
		System.out.println("SolverContinuo.eliminacionSimetrias = " + valor);
		_eliminacionSimetrias = valor;
	}
	public void setCutPool(boolean valor)
	{
		System.out.println("SolverContinuo.cutPool = " + valor);
		_cutPool = valor;
	}
	public void setObjetivo(Modelo.Objetivo valor)
	{
		System.out.println("SolverContinuo.objetivo = " + valor);
		_objetivo = valor;
	}
	public void setTiempoMaximo(double valor)
	{
		System.out.println("SolverContinuo.tiempoMaximo = " + valor);
		_tiempoMaximo = valor;
	}
	public void setPads(int valor)
	{
		System.out.println("SolverContinuo.pads = " + valor);
		_pads = valor;
	}
	
	public Solucion resolver()
	{
		Solucion solucion = null;
		System.out.println();
		
		try
		{
			_cplex = new IloCplex();
			_cplex.setParam(IloCplex.IntParam.TimeLimit, _tiempoMaximo);

			_modelo = new Modelo(_instancia, _cplex, _pads);
			_modelo.setObjetivo(_objetivo);
			_modelo.setEliminacionSimetrias(_eliminacionSimetrias);
			_modelo.generar();
			
//			_cplex.exportModel("modelo.lp");
			
			if( _cutPool == true )
				GeneradorCliques.ejecutar(_cplex, _modelo);

			_cplex.use(new Separador(_cplex, _modelo));
			
			if( _cplex.solve() )
				solucion = construirSolucion();
			
			_objvalue = _cplex.getObjValue();
			_gap = 100 * _cplex.getMIPRelativeGap();
			_status = _cplex.getStatus().toString();
			_nodes = _cplex.getNnodes();
			_cuts = _cplex.getNUCs();

			System.out.println();
			System.out.println("CPLEX Obj value: " + _objvalue);
			System.out.println("CPLEX Gap: " + _gap);
			System.out.println("CPLEX Status: " + _status);
			System.out.println("CPLEX Nodes: " + _nodes);
			System.out.println("CPLEX User Cuts: " + _cuts);
			
			
			GeneradorCliques.mostrarEstadisticas();
			Separador.mostrarEstadisticas();
		}
		catch (IloException e)
		{
			System.err.println("Concert exception caught: " + e);
			e.printStackTrace();
		}
		
		return solucion;
	}
	
	private Solucion construirSolucion() throws UnknownObjectException, IloException
	{
		Solucion solucion = new Solucion(_instancia);
		
		for(int i=0; i < _modelo.getPads(); ++i)
		for(int k=0; k < _instancia.getSemillas().size(); ++k) if( _cplex.getValue(_modelo.getw(i,k)) > 0.9 )
		{
			double x = _cplex.getValue(_modelo.getx(i));
			double y = _cplex.getValue(_modelo.gety(i));
			
			solucion.agregarPad( Pad.rigido(_instancia, _instancia.getSemillas().get(k), new Coordinate(x,y)) );
		}
		
		return solucion;
	}
	
	public double getObjValue()
	{
		return _objvalue;
	}
	
	public double getGap()
	{
		return _gap;
	}
	
	public String getStatus()
	{
		return _status;
	}
	
	public int getNodes()
	{
		return _nodes;
	}
	
	public int getUserCuts()
	{
		return _cuts;
	}
}
