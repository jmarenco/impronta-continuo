package colgen;

import java.util.ArrayList;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

import impronta.Solucion;
import impronta.Discretizacion;
import impronta.Instancia;
import impronta.Pad;
import impronta.Timer;

// Representa el proceso de resolucion
public class HeuristicaInicial
{
	private Instancia _instancia;
	private Discretizacion _discretizacion;
	private ArrayList<Pad> _pads;
	private CplexCG _cplex;

	// Constructor
	public HeuristicaInicial(Instancia instancia)
	{
		_instancia = instancia;
	}

	// Ejecuta la heurística
	public static Solucion ejecutar(Instancia instancia)
	{
		return new HeuristicaInicial(instancia).ejecutar();
	}

	// Ejecuta la heurística
	public Solucion ejecutar()
	{
		construirDiscretizacion();
		generarPads();
		inicializarModelo();

		return resolverRelajacion();
	}

	// Construye la discretizacion
	private void construirDiscretizacion()
	{
		_instancia.setPasoHorizontal(_instancia.getSemillas().stream().mapToDouble(s -> s.getLargo()).min().orElse(100));
		_instancia.setPasoVertical(_instancia.getSemillas().stream().mapToDouble(s -> s.getAncho()).min().orElse(100));

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
	private void generarPads()
	{
		System.out.println("Construyendo pads ...");
		System.out.println();
		
		_pads = _discretizacion.construirPads();
		Timer.chequear();
		
		System.out.println("  -> " + _pads.size() + " pads generados");
		System.out.println();
	}
	
	// Inicializa el modelo
	private void inicializarModelo()
	{
		_cplex = new CplexCG();
		
		for(Pad pad: _pads)
		{
			_cplex.agregarVariable(pad, objetivo(pad));
			_cplex.agregarRestriccion(pad.getCentro());

			Coordinate centro = pad.getCentro().getCoordinate();
			for(Coordinate esquina: pad.getPerimetro().getCoordinates())
			{
				double x = esquina.x < centro.x ? esquina.x + _instancia.getPasoHorizontal() : esquina.x - _instancia.getPasoHorizontal();
				double y = esquina.y < centro.y ? esquina.y + _instancia.getPasoVertical() : esquina.y - _instancia.getPasoVertical();

				_cplex.agregarRestriccion(_instancia.getFactory().createPoint(new Coordinate(x,y)));
			}
		}
	}
	
	// Coeficiente de la funcion objetivo asociado con cada pad
	public double objetivo(Pad pad)
	{
		return pad.getArea();
	}
	
	// Resuelve el modelo y entrega la solución
	private Solucion resolverRelajacion()
	{
		_cplex.resolver();

		Map<Pad, Double> primales = _cplex.primales();
		Solucion ret = new Solucion(_instancia);
		
		for(Pad pad: primales.keySet()) if( primales.get(pad) > 0.49 )
			ret.agregarPad(pad);
		
		return ret;
	}
	
	// Consultas
	public Map<Pad, Double> getPrimales()
	{
		return _cplex.primales();
	}
	
	public Discretizacion getDiscretizacion()
	{
		return _discretizacion;
	}
}
