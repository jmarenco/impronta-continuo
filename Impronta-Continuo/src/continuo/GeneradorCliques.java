package continuo;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;
import impronta.Instancia;
import impronta.Semilla;

public class GeneradorCliques
{
	private IloCplex _cplex;
	private Modelo _modelo;
	private Instancia _instancia;
	
	public static void ejecutar(IloCplex cplex, Modelo modelo)
	{
		try
		{
			GeneradorCliques generador = new GeneradorCliques(cplex, modelo);
			generador.ejecutar();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public GeneradorCliques(IloCplex cplex, Modelo modelo)
	{
		_cplex = cplex;
		_modelo = modelo;
		_instancia = modelo.getInstancia();
		_semilla = _instancia.getSemillas().get(0);
		
		M = _modelo.maxx() - _modelo.minx();
	}
	
	private static int _cortesHorizontales;
	private static int _cortesVerticales;
	
	private double M;
	private Semilla _semilla;
	
	public static void inicializar()
	{
		_cortesHorizontales = 0;
		_cortesVerticales = 0;
	}
	
	public void ejecutar() throws IloException
	{
		if( _instancia.getSemillas().size() != 1 )
			return;
		
		generarHorizontales();
		generarVerticales();
	}
	
	private void generarHorizontales() throws IloException
	{
		for(int i=0; i<_modelo.getPads(); ++i)
		for(int j=i+2; j<i+5 && j<_modelo.getPads(); ++j)
		{
			IloNumExpr lhs = _cplex.linearNumExpr();
			double rhs = M - _semilla.getLargo();

			lhs = _cplex.sum(lhs, _cplex.prod(1.0, _modelo.getx(i)));
			lhs = _cplex.sum(lhs, _cplex.prod(-1.0, _modelo.getx(j)));
			lhs = _cplex.sum(lhs, _cplex.prod(M, _modelo.getl(i,j)));
			
			for(int k=i+1; k<j; ++k)
			{
				lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getLargo(), _modelo.getl(i,k)));
				lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getLargo(), _modelo.getl(k,j)));
				rhs += _semilla.getLargo();
			}
			
			_cplex.addUserCut(_cplex.le(lhs, rhs));
			_cortesHorizontales += 1;
		}
	}
	
	private void generarVerticales() throws IloException
	{
		for(int i=0; i<_modelo.getPads(); ++i)
		for(int j=i+2; j<i+5 && j<_modelo.getPads(); ++j)
		{
			IloNumExpr lhs = _cplex.linearNumExpr();
			double rhs = M - _semilla.getAncho();

			lhs = _cplex.sum(lhs, _cplex.prod(1.0, _modelo.gety(i)));
			lhs = _cplex.sum(lhs, _cplex.prod(-1.0, _modelo.gety(j)));
			lhs = _cplex.sum(lhs, _cplex.prod(M, _modelo.gett(i,j)));
			
			for(int k=i+1; k<j; ++k)
			{
				lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getAncho(), _modelo.gett(i,k)));
				lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getAncho(), _modelo.gett(k,j)));
				rhs += _semilla.getAncho();
			}
			
			_cplex.addUserCut(_cplex.le(lhs, rhs));
			_cortesVerticales += 1;
		}
	}
	
	public static void mostrarEstadisticas()
	{
		System.out.println();
		System.out.print(" -> GenClique = Horiz: " + _cortesHorizontales);
		System.out.println(", Vert: " + _cortesVerticales);
	}
	
	public static int getCortesHorizontales()
	{
		return _cortesHorizontales;
	}
	
	public static int getCortesVerticales()
	{
		return _cortesVerticales;
	}
}
