package continuo;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;

public class SeparadorVertical extends SeparadorGenerico
{
	public SeparadorVertical(Modelo modelo, Separador padre)
	{
		super(modelo, padre);
		
		_activo = _semillas > 1;
		_maxAncho = _instancia.getSemillas().stream().mapToDouble(s -> s.getAncho()).max().orElse(0);
		
		M = _modelo.maxy() - _modelo.miny();
	}

	private static double _epsilon = 1e-4;
	private static int _activaciones;
	private static int _cortes;

	private double _maxAncho;
	private boolean _activo = true;
	private double M;
	
	public static void inicializar()
	{
		_activaciones = 0;
		_cortes = 0;
	}
	
	@Override public void run(Solucion solucion) throws IloException
	{
		if( _activo == false )
			return;
		
		++_activaciones;
		
		for(int i=0; i<_pads; ++i)
		for(int j=i+1; j<_pads; ++j)
		for(int t=i+1; t<j; ++t)
		{
			double lhs = solucion.gety(i) - solucion.gety(j) + M * solucion.gett(i, j);
			
			for(int k=0; k<_semillas; ++k)
				lhs += (_instancia.getSemilla(k).getAncho() / 2) * (solucion.getw(i,k) + solucion.getw(j,k));
			
			double aporte = _maxAncho * (solucion.gett(i,t) + solucion.gett(t,j) - 2);
			for(int k=0; k<_semillas; ++k)
				aporte += _instancia.getSemilla(k).getAncho() * solucion.getw(t,k);
				
			lhs += aporte;
			
			if (lhs > M + _epsilon )
				agregarDesigualdad(i, j, t);
		}
	}

	// Agrega la desigualdad
	private void agregarDesigualdad(int i, int j, int t) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		double rhs = M;

		lhs = _cplex.sum(lhs, _cplex.prod(1.0, _modelo.gety(i)));
		lhs = _cplex.sum(lhs, _cplex.prod(-1.0, _modelo.gety(j)));
		lhs = _cplex.sum(lhs, _cplex.prod(M, _modelo.gett(i,j)));
		
		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getAncho() / 2, _modelo.getw(i,k)));
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getAncho() / 2, _modelo.getw(j,k)));
		}

		lhs = _cplex.sum(lhs, _cplex.prod(_maxAncho, _modelo.gett(i,t)));
		lhs = _cplex.sum(lhs, _cplex.prod(_maxAncho, _modelo.gett(t,j)));
		rhs += 2 * _maxAncho;
			
		for(int k=0; k<_semillas; ++k)
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getAncho(), _modelo.getw(t,k)));
		
		_padre.agregar(_cplex.le(lhs, rhs));
		_cortes++;
	}
	
	public static void mostrarEstadisticas()
	{
		System.out.print(" -> GenClique Vert = Act: " + _activaciones);
		System.out.println(", Cortes: " + _cortes);
	}
	
	public static String getResumen()
	{
		return _cortes + " / " + _activaciones + " | ";
	}
	
	public static int getActivaciones()
	{
		return _activaciones;
	}
	
	public static int getCortes()
	{
		return _cortes;
	}
}
