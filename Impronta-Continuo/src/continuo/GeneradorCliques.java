package continuo;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;
import impronta.Instancia;

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
		_maxLargo = _instancia.getSemillas().stream().mapToDouble(s -> s.getLargo()).max().orElse(0);
		_maxAncho = _instancia.getSemillas().stream().mapToDouble(s -> s.getAncho()).max().orElse(0);
		
		M = Math.max(_modelo.maxx() - _modelo.minx(), _modelo.maxy() - _modelo.miny());
	}
	
	private static int _cortesHorizontales;
	private static int _cortesVerticales;
	
	private double M;
	private double _maxLargo;
	private double _maxAncho;
	
	public static void inicializar()
	{
		_cortesHorizontales = 0;
		_cortesVerticales = 0;
	}
	
	public void ejecutar() throws IloException
	{
		generarHorizontales();
		generarVerticales();
	}
	
	private void generarHorizontales() throws IloException
	{
		for(int i=0; i<_modelo.getPads(); ++i)
		for(int j=i+2; j<i+5 && j<_modelo.getPads(); ++j)
		{
			IloNumExpr lhs = _cplex.linearNumExpr();
			double rhs = M;

			lhs = _cplex.sum(lhs, _cplex.prod(1.0, _modelo.getx(i)));
			lhs = _cplex.sum(lhs, _cplex.prod(-1.0, _modelo.getx(j)));
			lhs = _cplex.sum(lhs, _cplex.prod(M, _modelo.getl(i,j)));
			
			for(int s=0; s<_instancia.getSemillas().size(); ++s)
			{
				lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(s).getLargo()/2, _modelo.getw(i,s)));
				lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(s).getLargo()/2, _modelo.getw(j,s)));
			}
			
			for(int k=i+1; k<i+2; ++k) // Solamente un pad!
			{
				lhs = _cplex.sum(lhs, _cplex.prod(_maxLargo, _modelo.getl(i,k)));
				lhs = _cplex.sum(lhs, _cplex.prod(_maxLargo, _modelo.getl(k,j)));
				rhs += 2 * _maxLargo;

				for(int s=0; s<_instancia.getSemillas().size(); ++s)
					lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(s).getLargo(), _modelo.getw(k,s)));
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
			double rhs = M;

			lhs = _cplex.sum(lhs, _cplex.prod(1.0, _modelo.gety(i)));
			lhs = _cplex.sum(lhs, _cplex.prod(-1.0, _modelo.gety(j)));
			lhs = _cplex.sum(lhs, _cplex.prod(M, _modelo.gett(i,j)));
			
			for(int s=0; s<_instancia.getSemillas().size(); ++s)
			{
				lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(s).getAncho()/2, _modelo.getw(i,s)));
				lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(s).getAncho()/2, _modelo.getw(j,s)));
			}

			for(int k=i+1; k<i+2; ++k) // Solamente un pad!
			{
				lhs = _cplex.sum(lhs, _cplex.prod(_maxAncho, _modelo.gett(i,k)));
				lhs = _cplex.sum(lhs, _cplex.prod(_maxAncho, _modelo.gett(k,j)));
				rhs += 2 * _maxAncho;

				for(int s=0; s<_instancia.getSemillas().size(); ++s)
					lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(s).getAncho(), _modelo.getw(k,s)));
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
