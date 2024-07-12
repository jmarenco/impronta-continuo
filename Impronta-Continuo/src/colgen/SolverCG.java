package colgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import impronta.Discretizacion;
import impronta.Instancia;
import impronta.Pad;
import impronta.Timer;

// Representa el proceso de resolucion
public class SolverCG
{
	private Instancia _instancia;
	private Discretizacion _discretizacion;
	private ArrayList<Pad> _pads;
	private CplexCG _cplex;
	private Dualizer _dualizer;

	private int _iteracion;
	private boolean _dualOptimo;
	private boolean _dualViolado;
	private boolean _primalViolado;
	private Pad _violadorDual;
	
	private Random _random = new Random(5);

	// Constructor
	public SolverCG(Instancia instancia)
	{
		_instancia = instancia;
		_iteracion = 0;
	}
	
	// Resuelve la instancia
	public void iniciar()
	{
		construirDiscretizacion();
		generarPads();
		inicializarModelo();
		resolverRelajacion();
	}
	
	public boolean iterar()
	{
		_iteracion++;
		_dualOptimo = false;
		_dualViolado = false;
		_primalViolado = false;
		_violadorDual = null;
		
		Point point = lazyConstraint();
		if( point != null )
		{
			agregar(point);
			resolverRelajacion();
			
			_primalViolado = true;
			return true;
		}

		if( dualOptimo() == true )
		{
			_dualOptimo = true;
			return false;
		}
		
		Pad pad = violadorDual();
		if( pad != null )
		{
			agregar(pad);
			resolverRelajacion();
			
			_dualViolado = true;
			_violadorDual = pad;
			return true;
		}
		
		return false;
	}
	
	@Deprecated
	public boolean iterar_previo()
	{
		_dualOptimo = false;
		_dualViolado = false;
		_primalViolado = false;
		_violadorDual = null;
		
		if( dualOptimo() == true )
		{
			_dualOptimo = true;
			
			Point point = lazyConstraint();
			if( point == null )
				return false;
			
			agregar(point);
			resolverRelajacion();
				
			_primalViolado = true;
			return true;
		}
		
		Pad pad = violadorDual();
		if( pad != null )
		{
			agregar(pad);
			resolverRelajacion();
			
			_dualViolado = true;
			_violadorDual = pad;
			return true;
		}
		
		Point point = lazyConstraint();
		if( point != null )
		{
			agregar(point);
			resolverRelajacion();
			
			_primalViolado = true;
			return true;
		}
		
		return false;
	}

	// Construye la discretizacion
	private void construirDiscretizacion()
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
		agregar(_pads.get(0));
	}
	
	// Pruebas de la resolucion
	private void resolverRelajacion()
	{
		_cplex.resolver();
	}
	
	// Agrega un pad y sus restricciones al modelo
	private void agregar(Pad pad)
	{
		_cplex.agregarVariable(pad, objetivo(pad));

		Coordinate centro = pad.getCentro().getCoordinate();
		for(Coordinate esquina: pad.getPerimetro().getCoordinates())
		{
			double x = esquina.x < centro.x ? esquina.x + _instancia.getPasoHorizontal() : esquina.x - _instancia.getPasoHorizontal();
			double y = esquina.y < centro.y ? esquina.y + _instancia.getPasoVertical() : esquina.y - _instancia.getPasoVertical();
			double z = esquina.z;

			agregar(_instancia.getFactory().createPoint(new Coordinate(x,y,z)));
		}

		agregar(pad.getCentro());
	}
	
	// Agrega la restriccion asociada con un punto
	private void agregar(Point point)
	{
		_cplex.agregarRestriccion(point);
	}
	
	// Busca un pad que viole las restricciones duales
	public Pad violadorDual()
	{
		Map<Point, Double> duales = _cplex.duales();
		Collections.shuffle(_pads, _random);
		
		for(Pad pad: _pads)
		{
			double lhs = 0;
			for(Point point: duales.keySet()) if( pad.contiene(point) )
				lhs += duales.get(point);
			
			if( lhs < 0.99 * objetivo(pad) )
				return pad;
		}
		
		return null;
	}
	
	// Busca un punto cubierto por mas de un pad
	private Point lazyConstraint()
	{
		Map<Pad, Double> primales = _cplex.primales();
		
		for(Pad pad: primales.keySet())
		for(Coordinate punto: pad.getEsquinas())
		{
			double offsetx = punto.x > pad.getCentro().getX() ? -_instancia.getPasoHorizontal() : _instancia.getPasoHorizontal(); 
			double offsety = punto.y > pad.getCentro().getY() ? -_instancia.getPasoVertical() : _instancia.getPasoVertical();
			
			Coordinate interno = new Coordinate(punto.x + offsetx, punto.y + offsety);
			
			double lhs = 0;
			for(Pad variable: primales.keySet()) if( variable.contiene(interno) )
				lhs += primales.get(variable);
			
			if( lhs > 1.01 )
				return _instancia.getFactory().createPoint(interno);
		}
		
		return null;
	}
	private Point lazyConstraint_previo()
	{
		Map<Pad, Double> primales = _cplex.primales();
		
		List<Point> points = Arrays.asList(_discretizacion.getPoints());
		Collections.shuffle(points, _random);
		
		for(Point point: points)
		{
			double lhs = 0;
			for(Pad pad: primales.keySet()) if( pad.contiene(point) )
				lhs += primales.get(pad);
			
			if( lhs > 1.01 )
				return point;
		}
		
		return null;
	}
	
	// Intenta construir una solucion dual con el mismo objetivo que el primal
	private boolean dualOptimo()
	{
		_dualizer = new Dualizer(this, _random);
		return _dualizer.esOptima();
	}
	
	// Coeficiente de la funcion objetivo asociado con cada pad
	public double objetivo(Pad pad)
	{
		return pad.getArea();
	}
	
	// Consultas sobre la solucion
	public Map<Pad, Double> primales()
	{
		return _cplex.primales();
	}
	public Map<Point, Double> duales()
	{
		return _cplex.duales();
	}
	
	// Consultas sobre los datos intermedios
	public Instancia getInstancia()
	{
		return _instancia;
	}
	public Discretizacion getDiscretizacion()
	{
		return _discretizacion;
	}
	public ArrayList<Pad> getPads()
	{
		return _pads;
	}
	public CplexCG getCplex()
	{
		return _cplex;
	}
	public Dualizer getDualizer()
	{
		return _dualizer;
	}
	public int getIteracion()
	{
		return _iteracion;
	}
	public boolean esDualOptimo()
	{
		return _dualOptimo;
	}
	public boolean esDualViolado()
	{
		return _dualViolado;
	}
	public boolean esPrimalViolado()
	{
		return _primalViolado;
	}
	public Pad getVioladorDual()
	{
		return _violadorDual;
	}
}
