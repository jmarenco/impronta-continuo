package continuo;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;

public class SeparadorHorizontal extends SeparadorGenerico
{
	public SeparadorHorizontal(Modelo modelo, Separador padre)
	{
		super(modelo, padre);
		
		_activo = _semillas > 1;
		_maxLargo = _instancia.getSemillas().stream().mapToDouble(s -> s.getLargo()).max().orElse(0);
		
		M = _modelo.maxx() - _modelo.minx();
	}

	private static double _epsilon = 1e-4;
	private static int _activaciones;
	private static int _cortes;

	private double _maxLargo;
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
		for(int j=i+2; j<_pads; ++j)
		for(int t=i+1; t<j; ++t)
		{
			double lhs = solucion.getx(i) - solucion.getx(j) + M * solucion.getl(i, j);
			
			for(int k=0; k<_semillas; ++k)
				lhs += (_instancia.getSemilla(k).getLargo() / 2) * (solucion.getw(i,k) + solucion.getw(j,k));
			
			double aporte = _maxLargo * (solucion.getl(i,t) + solucion.getl(t,j) - 2);
			for(int k=0; k<_semillas; ++k)
				aporte += _instancia.getSemilla(k).getLargo() * solucion.getw(t,k);
				
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

		lhs = _cplex.sum(lhs, _cplex.prod(1.0, _modelo.getx(i)));
		lhs = _cplex.sum(lhs, _cplex.prod(-1.0, _modelo.getx(j)));
		lhs = _cplex.sum(lhs, _cplex.prod(M, _modelo.getl(i,j)));
		
		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getLargo() / 2, _modelo.getw(i,k)));
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getLargo() / 2, _modelo.getw(j,k)));
		}

		lhs = _cplex.sum(lhs, _cplex.prod(_maxLargo, _modelo.getl(i,t)));
		lhs = _cplex.sum(lhs, _cplex.prod(_maxLargo, _modelo.getl(t,j)));
		rhs += 2 * _maxLargo;
			
		for(int k=0; k<_semillas; ++k)
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getLargo(), _modelo.getw(t,k)));
		
		_padre.agregar(_cplex.le(lhs, rhs));
		_cortes++;
	}
	
	public static void mostrarEstadisticas()
	{
		System.out.print(" -> GenClique Horiz = Act: " + _activaciones);
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
