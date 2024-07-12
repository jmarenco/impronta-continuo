package testing;

import impronta.Instancia;
import impronta.Region;
import impronta.Semilla;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class Instancer
{
	public static Instancia getInstancia()
	{
		// Construye una instancia de prueba
	    Coordinate[] coordinates = new Coordinate[7];
	    coordinates[0] = new Coordinate(0,0);
	    coordinates[1] = new Coordinate(0,400);
	    coordinates[2] = new Coordinate(400,400);
	    coordinates[3] = new Coordinate(400,100);
	    coordinates[4] = new Coordinate(500,100);
	    coordinates[5] = new Coordinate(500,0);
	    coordinates[6] = new Coordinate(0,0);
	    
	    GeometryFactory fact = new GeometryFactory();
		Region region = new Region();
		region.agregarEnvolvente(fact.createPolygon(coordinates));
		
		Instancia instancia = new Instancia();
		instancia.setRegion(region);
		instancia.setAngulo(Math.PI / 5);
		instancia.setPasoHorizontal(10);
		instancia.setPasoVertical(10);
		instancia.agregarSemilla(new Semilla(20, 30, 5, 5, 5, 1));
		instancia.agregarSemilla(new Semilla(40, 60, 5, 5, 5, 1));
		
		return instancia;
	}
}
