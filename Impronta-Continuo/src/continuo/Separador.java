package continuo;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CutManagement;

public class Separador extends IloCplex.UserCutCallback
{
	private Modelo _modelo;
	private ArrayList<SeparadorGenerico> _separadores;
	
	private static int _activaciones = 0;
	private static int _llamadas = 0;
	private static int _skipFactor = 100;
	
	public Separador(IloCplex cplex, Modelo modelo)
	{
		_modelo = modelo;

		_separadores = new ArrayList<SeparadorGenerico>();
		_separadores.add( new SeparadorCliqueHorizontal(modelo, this) );
		_separadores.add( new SeparadorCliqueVertical(modelo, this) );
		_separadores.add( new SeparadorGenCliqueHorizontal(modelo, this) );
		_separadores.add( new SeparadorGenCliqueVertical(modelo, this) );
	}
	
	@Override
	protected void main() throws IloException
	{
		if(!this.isAfterCutLoop())
	        return;
		
		if( this.getNnodes() > 1 )
			return;
		
		if( _llamadas % _skipFactor != 0 )
			return;
		
		_llamadas += 1;
		
		Solucion solucion = new Solucion(_modelo, this);
		for(SeparadorGenerico separador: _separadores)
			separador.run(solucion);
		
//		if( _activaciones == 0 )
//			System.out.println(solucion);
		
		_activaciones += 1;
	}
	
	public double getValor(IloNumVar variable) throws IloException
	{
		return this.getValue(variable);
		
	}
	public void agregar(IloRange cut) throws IloException
	{
		this.add(cut, CutManagement.UseCutForce);
	}
	
	public static void mostrarEstadisticas()
	{
		System.out.println(" -> Separacion = Llamadas: " + _llamadas + ", Act: " + _activaciones);
		
		SeparadorCliqueHorizontal.mostrarEstadisticas();
		SeparadorCliqueVertical.mostrarEstadisticas();
	}
	
	public static int getLlamadas()
	{
		return _llamadas;
	}
	
	public static int getActivaciones()
	{
		return _activaciones;
	}
}
