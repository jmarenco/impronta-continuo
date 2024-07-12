package impronta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Polygon;

// Representa un sampleo de "original gas in place"
public class OGIP
{
	// Mediciones
	private Map<Coordinate, Double> _mediciones;
	
	// Dominio de las mediciones
	private MultiPoint _puntos;
	
	// Constructor
	public OGIP()
	{
		_mediciones = new HashMap<Coordinate, Double>();
		_puntos = null;
	}
	
	// Agrega una medición
	public void agregar(Coordinate c, double valor)
	{
		if( _mediciones.containsKey(c) )
		{
			System.out.println( "  -> Error! Ya existe una medición OGIP para el punto (" + c.x + ", " + c.y + ")");
			return;
		}
		
		_mediciones.put(c, valor);
	}
	
	// Suma de las mediciones incluidas dentro del poligono
	public double valor(Polygon polygon)
	{
		// Si es la primera vez que se llama, construye el MultiPoint con los puntos de las mediciones
		if( _puntos == null )
		{
			System.out.println("  -> Construyendo conjunto de puntos OGIP");
			_puntos = polygon.getFactory().createMultiPoint(_mediciones.keySet().toArray(new Coordinate[] {}));
			
			System.out.println("  -> Conjunto construido");
			System.out.println();
			Timer.chequear();
		}
		
		// Suma los valores de los puntos incluidos dentro del polígono
		double ret = 0;
		for(Coordinate c: _puntos.intersection(polygon).getCoordinates())
			ret += _mediciones.containsKey(c) ? _mediciones.get(c) : 0.0;
			
		return ret;
	}

	// Cantidad de mediciones
	public int getCantidad()
	{
		return _mediciones.size();
	}
	
	// Ubicaciones de las mediciones
	public Set<Coordinate> getPuntos()
	{
		return _mediciones.keySet();
	}
	
	// Getter
	public double getValor(Coordinate punto)
	{
		return _mediciones.containsKey(punto) ? _mediciones.get(punto) : 0.0;
	}
	
	// Máxima y mínima medición
	public double maxMedicion()
	{
		return Collections.max(_mediciones.values());
	}
	public double minMedicion()
	{
		return Collections.min(_mediciones.values());
	}
}
