package continuo;

import java.text.DecimalFormat;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;

public class SeparadorGenCliqueHorizontal extends SeparadorGenerico
{
	public SeparadorGenCliqueHorizontal(Modelo modelo, Separador padre)
	{
		super(modelo, padre);
		
		_activo = _semillas > 1;
		_maxLargo = _instancia.getSemillas().stream().mapToDouble(s -> s.getLargo()).max().orElse(0);
		
		M = _modelo.maxx() - _modelo.minx();
	}

	private static double _epsilon = 1e-4;
	private static int _activaciones;
	private static int _cortes;
	private static int _cliques;

	private double _maxLargo;
	private boolean _activo = true;
	private double M;
	
	public static void inicializar()
	{
		_activaciones = 0;
		_cortes = 0;
		_cliques = 0;
	}
	
	@Override public void run(Solucion solucion) throws IloException
	{
		if( _activo == false )
			return;
		
		++_activaciones;
		
		for(int i=0; i<_pads; ++i)
		for(int j=i+1; j<_pads; ++j)
		{
			double lhs = solucion.getx(i) - solucion.getx(j) + M * solucion.getl(i, j);
			boolean[] K = new boolean[_pads];
			
			for(int k=0; k<_semillas; ++k)
				lhs += (_instancia.getSemilla(k).getLargo() / 2) * (solucion.getw(i,k) + solucion.getw(j,k));
			
			for(int t=i+1; t<j; ++t)
			{
				double aporte = _maxLargo * (solucion.getl(i,t) + solucion.getl(t,j) - 2);
				for(int k=0; k<_semillas; ++k)
					aporte += _instancia.getSemilla(k).getLargo() * solucion.getw(t,k);
				
				if( aporte > 0 )
				{
					lhs += aporte;
					K[t] = true;
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
		
		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getLargo() / 2, _modelo.getw(i,k)));
			lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getLargo() / 2, _modelo.getw(j,k)));
		}

		for(int t=0; t<_pads; ++t) if( K[t] == true )
		{
			lhs = _cplex.sum(lhs, _cplex.prod(_maxLargo, _modelo.getl(i,t)));
			lhs = _cplex.sum(lhs, _cplex.prod(_maxLargo, _modelo.getl(t,j)));
			rhs += 2 * _maxLargo;
			
			for(int k=0; k<_semillas; ++k)
				lhs = _cplex.sum(lhs, _cplex.prod(_instancia.getSemilla(k).getLargo(), _modelo.getw(t,k)));
			
			_cliques++;
		}
		
		_padre.agregar(_cplex.le(lhs, rhs));
		_cortes++;
	}
	
	public static void mostrarEstadisticas()
	{
		DecimalFormat formato = new DecimalFormat("##0.00");
		
		System.out.print(" -> GenClique Horiz = Act: " + _activaciones);
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
