package colgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import impronta.Instancia;
import impronta.Pad;
import impronta.Semilla;

// Representa el proceso de resolucion
public class SolverCG
{
	private Instancia _instancia;
	private ArrayList<Pad> _pads;
	private CplexCG _cplex;
	private Dualizer _lastDualizer;

	private int _iteracion;
	private boolean _dualViolado;

	// Constructor
	public SolverCG(Instancia instancia)
	{
		_instancia = instancia;
		_iteracion = 0;
	}
	
	// Resuelve la instancia
	public void resolver()
	{
		resolver(Integer.MAX_VALUE);
	}
	
	public void resolver(int iteraciones)
	{
		inicializarEstadisticas();
		inicializarPads();
		inicializarModelo();
		
		for(int i=0; i<iteraciones && _dualViolado; ++i)
		{
			resolverRelajacion();
			boolean addedConstraints = lazyConstraints();
			
			while( addedConstraints == true )
			{
				resolverRelajacion();
				addedConstraints = lazyConstraints();
			}
			
			_dualViolado = dualizar();
			_iteracion++;
		}
	}
	
	// Inicializa las estadísticas
	private void inicializarEstadisticas()
	{
		_iteracion = 0;
		_dualViolado = true;
	}
	
	// Inicializa los pads con la heurística inicial
	private void inicializarPads()
	{
		_pads = HeuristicaInicial.ejecutar(_instancia).getPads();
		System.out.println(_pads.size() + " pads iniciales");
	}
	
	// Inicializa el modelo
	private void inicializarModelo()
	{
		_cplex = new CplexCG();
		
		for(Pad pad: _pads)
			agregarVariable(pad);
	}
	
	// Pruebas de la resolucion
	private void resolverRelajacion()
	{
		_cplex.resolver();
	}
	
	// Busca un punto cubierto por mas de un pad y agrega la restricción asociada
	private boolean lazyConstraints()
	{
		// TODO: Buscar mas de un punto!
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
			{
				agregarRestriccion(_instancia.getFactory().createPoint(interno));
				return true;
			}
		}
		
		return false;
	}
	
	// Resuelve el problema dualizador, busca un punto no cubierto, y agrega la variable asociada
	public boolean dualizar()
	{
		_lastDualizer = new Dualizer(this);
		_lastDualizer.resolver();
		
		List<Point> uncovered = CubrimientoDual.getUncoveredPoints(_instancia, _lastDualizer.getCubrimiento());
		
		for(Point point: uncovered)
		for(Semilla semilla: _instancia.getSemillas())
			agregarVariable(Pad.flexible(_instancia, semilla, point.getCoordinate()));
		
		return uncovered.size() > 0;
	}
	
	// Agrega un pad y sus restricciones al modelo
	private void agregarVariable(Pad pad)
	{
		if( pad.factible(_instancia.getRegion()) )
			_cplex.agregarVariable(pad, objetivo(pad));

		Coordinate centro = pad.getCentro().getCoordinate();
		for(Coordinate esquina: pad.getPerimetro().getCoordinates())
		{
			double x = esquina.x < centro.x ? esquina.x + _instancia.getPasoHorizontal() : esquina.x - _instancia.getPasoHorizontal();
			double y = esquina.y < centro.y ? esquina.y + _instancia.getPasoVertical() : esquina.y - _instancia.getPasoVertical();
			double z = esquina.z;

			agregarRestriccion(_instancia.getFactory().createPoint(new Coordinate(x,y,z)));
		}

		agregarRestriccion(pad.getCentro());
	}
	
	// Agrega la restriccion asociada con un punto
	private void agregarRestriccion(Point point)
	{
		_cplex.agregarRestriccion(point);
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
	public ArrayList<Pad> getPads()
	{
		return _pads;
	}
	public CplexCG getCplex()
	{
		return _cplex;
	}
	public Dualizer getLastDualizer()
	{
		return _lastDualizer;
	}
	public int getIteracion()
	{
		return _iteracion;
	}
}
