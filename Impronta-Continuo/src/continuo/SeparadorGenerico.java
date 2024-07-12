package continuo;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import impronta.Instancia;

public abstract class SeparadorGenerico
{
	protected IloCplex _cplex;
	protected Modelo _modelo;
	protected Instancia _instancia;
	protected Separador _padre;

	protected int _pads;
	protected int _semillas;

	public SeparadorGenerico(Modelo modelo, Separador padre)
	{
		_modelo = modelo;
		_cplex = modelo.getCplex();
		_instancia = modelo.getInstancia();
		_modelo = modelo;
		_padre = padre;
		
		_pads = _modelo.getPads();
		_semillas = _instancia.getSemillas().size();
	}
	
	public abstract void run(Solucion solucion) throws IloException;
}
