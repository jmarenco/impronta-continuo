package impronta;

import com.vividsolutions.jts.geom.Polygon;

// Representa una restriccion
public class Restriccion
{
	// Datos privados
	private String _id;
	private String _ring;
	private Polygon _poligono;
	
	// Construye la region
	public Restriccion(String id, String ring, Polygon poligono)
	{
		_id = id;
		_ring = ring;
		_poligono = poligono;
	}

	// Determina si interseca el poligono especificado
	public boolean interseca(Polygon poligono)
	{
		return _poligono.intersects(poligono);
	}
	
	// Obtiene un identificador
	public String identificacion()
	{
		return "ID: " + _id + ", Ring: " + _ring;
	}

	// Getter
	public Polygon getPolygon()
	{
		return _poligono;
	}
}
