package impronta;

import java.util.ArrayList;
import java.util.HashSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

// Representa el proceso de resolucion
public abstract class SolverDiscreto
{
	protected Instancia _instancia;
	protected Discretizacion _discretizacion;
	protected ArrayList<Pad> _pads;
	protected Grafo _grafo;

	// Constructor
	public SolverDiscreto(Instancia instancia)
	{
		_instancia = instancia;
	}
	
	// Resuelve la instancia
	public abstract Solucion resolver();

	// Construye la discretizacion
	protected void construirDiscretizacion()
	{
		System.out.println("Construyendo discretizacion ...");
		System.out.println();
		System.out.println("  -> Delta x: " + _instancia.getPasoHorizontal() + ", Delta y: " + _instancia.getPasoVertical());
		Timer.comenzar();

		_discretizacion = new Discretizacion(_instancia);

		System.out.println("  -> " + _discretizacion.getPuntos().getCoordinates().length + " puntos generados");
		System.out.println();
		Timer.chequear();
	}

	// Genera todos los pads factibles
	protected void generarPads()
	{
		System.out.println("Construyendo pads ...");
		System.out.println();
		
		_pads = _discretizacion.construirPads();
		Timer.chequear();
		
		System.out.println("  -> " + _pads.size() + " pads generados");
		System.out.println();
	}

	// Construye el grafo de intersecciones
	protected void construirGrafo()
	{
		_grafo = new Grafo(_pads.size());
		
		for(int i=0; i<_pads.size(); ++i)
			_grafo.setPeso(i, _pads.get(i).getValorizacion());
		
		int k = 0;
		for(Coordinate c: _discretizacion.getPuntos().getCoordinates())
		{
			Point punto = _instancia.getFactory().createPoint(c);
			HashSet<Integer> vertices = new HashSet<Integer>();
			
			for(int i=0; i<_pads.size(); ++i) if( _pads.get(i).contiene(punto) )
				vertices.add(i);
			
			_grafo.agregarClique(vertices);
			
			if( (++k) % (_discretizacion.getPuntos().getNumPoints() / 20) == 0 )
			{
				System.out.println("  -> Grafo " + Math.round(k * 100.0 / _discretizacion.getPuntos().getNumPoints()) + "% construido" );
				Timer.chequear();
			}
		}

		System.out.println();
	}
	
	// Retorna la discretizacion	
	public Discretizacion getDiscretizacion()
	{
		return _discretizacion;
	}
}
