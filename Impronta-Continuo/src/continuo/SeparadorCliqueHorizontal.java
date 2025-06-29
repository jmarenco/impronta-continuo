package continuo;

import java.text.DecimalFormat;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import impronta.Semilla;

public class SeparadorCliqueHorizontal extends SeparadorGenerico
{
	public SeparadorCliqueHorizontal(Modelo modelo, Separador padre)
	{
		super(modelo, padre);
		
		M = _modelo.maxx() - _modelo.minx();
		_semilla = _instancia.getSemillas().get(0);
	}
	
	private static double _epsilon = 1e-1;
	private static int _activaciones;
	private static int _cortes;
	private static int _cliques;

	private double M;
	private Semilla _semilla;
	
	public static void inicializar()
	{
		_activaciones = 0;
		_cortes = 0;
		_cliques = 0;
	}
	
	@Override public void run(Solucion solucion) throws IloException
	{
		if( _semillas != 1 )
			return;
		
		++_activaciones;
		
		for(int i=0; i<_pads; ++i)
		for(int j=i+1; j<_pads; ++j)
		{
			double lhs = solucion.getx(i) - solucion.getx(j) + M * solucion.getl(i, j) + (_semilla.getLargo() / 2) * (solucion.getw(i,0) + solucion.getw(j,0));
			boolean[] K = new boolean[_pads];
			
			for(int k=i+1; k<j; ++k)
			{
				double aporte = _semilla.getLargo() * (solucion.getw(k,0) + solucion.getl(i,k) - 2 + solucion.getl(k,j));
				if( aporte > 0 )
				{
					lhs += aporte;
					K[k] = true;
				}
			}
			
			if (lhs > M + _epsilon )
				agregarDesigualdad(i, j, K);
		}
	}

	// Agrega la desigualdad
	private void agregarDesigualdad(int i, int j, boolean[] K) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		double rhs = M;

		lhs = _cplex.sum(lhs, _cplex.prod(1.0, _modelo.getx(i)));
		lhs = _cplex.sum(lhs, _cplex.prod(-1.0, _modelo.getx(j)));
		lhs = _cplex.sum(lhs, _cplex.prod(M, _modelo.getl(i,j)));
		lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getLargo() / 2, _modelo.getw(i,0)));
		lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getLargo() / 2, _modelo.getw(j,0)));
		
		for(int k=0; k<_pads; ++k) if( K[k] == true )
		{
			lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getLargo(), _modelo.getw(k,0)));
			lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getLargo(), _modelo.getl(i,k)));
			lhs = _cplex.sum(lhs, _cplex.prod(_semilla.getLargo(), _modelo.getl(k,j)));
			rhs += 2 * _semilla.getLargo();
			
			_cliques++;
		}
		
		_padre.agregar(_cplex.le(lhs, rhs));
		_cortes++;
	}
	
	public static void mostrarEstadisticas()
	{
		DecimalFormat formato = new DecimalFormat("##0.00");
		
		System.out.print(" -> Clique Horiz = Act: " + _activaciones);
		System.out.print(", Cortes: " + _cortes);
		System.out.println(", Clique prom.: " + formato.format(_cortes > 0 ? _cliques / (double)_cortes : 0) );
	}
	
	public static String getResumen()
	{
		return _cortes + " / " + _activaciones + " | " + String.format("%5.2f", _cortes > 0 ? _cliques / (double)_cortes : 0);
	}
	
	public static int getActivaciones()
	{
		return _activaciones;
	}
	
	public static int getCortes()
	{
		return _cortes;
	}
	
	public static double getCliquePromedio()
	{
		return _cortes > 0 ? _cliques / (double)_cortes : 0;
	}
}
