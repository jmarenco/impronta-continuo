package testing;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import continuo.RepresentacionLineal;

public class RepresentacionLinealTest
{
	@Test
	public void limitesTest()
	{
		RepresentacionLineal representacion = crear(xy(0,0), xy(2,0), xy(2,3));
		
		assertEquals(2, representacion.maxx(), 1e-4);
		assertEquals(0, representacion.minx(), 1e-4);
		assertEquals(3, representacion.maxy(), 1e-4);
		assertEquals(0, representacion.miny(), 1e-4);
	}
	
	@Test
	public void trianguloTest()
	{
		RepresentacionLineal representacion = crear(xy(0,0), xy(2,0), xy(0,2));
		
		assertEquals(3, representacion.restricciones());
		assertContiene(representacion, -1, 0, 0);
		assertContiene(representacion, 0, -1, 0);
		assertContiene(representacion, 1, 1, 2);
		
	}
	
	@Test
	public void trianguloDesplazadoTest()
	{
		RepresentacionLineal representacion = crear(xy(1,1), xy(3,1), xy(2,2));
		
		assertEquals(3, representacion.restricciones());
		assertContiene(representacion, 0, -1, -1);
		assertContiene(representacion, -1, 1, 0);
		assertContiene(representacion, 1, 1, 4);
		
	}

	@Test
	public void cuadradoTest()
	{
		RepresentacionLineal representacion = crear(xy(0,0), xy(2,0), xy(2,2), xy(0,2));
		
		assertEquals(4, representacion.restricciones());
		assertContiene(representacion, -1, 0, 0);
		assertContiene(representacion, 0, -1, 0);
		assertContiene(representacion, 1, 0, 2);
		assertContiene(representacion, 0, 1, 2);
	}

	private Coordinate xy(double x, double y)
	{
		return new Coordinate(x, y);
	}
	
	private RepresentacionLineal crear(Coordinate ...coordenadas)
	{
		Coordinate[] cerradas = new Coordinate[coordenadas.length + 1];
		
		for(int i=0; i<coordenadas.length; ++i)
			cerradas[i] = coordenadas[i];
		
		cerradas[coordenadas.length] = coordenadas[0];
		
		GeometryFactory factory = new GeometryFactory();
		return new RepresentacionLineal(factory.createPolygon(cerradas));
	}
	
	private void assertContiene(RepresentacionLineal representacion, double ax, double ay, double b)
	{
		for(int i=0; i<representacion.restricciones(); ++i)
		{
			double repax = representacion.get(i).ax;
			double repay = representacion.get(i).ay;
			double repb = representacion.get(i).b;
			
			if( (ax == 0 && repax != 0) || (ax != 0 && repax == 0) )
				continue;
			
			if( (ay == 0 && repay != 0) || (ay != 0 && repay == 0) )
				continue;
			
			if( (b == 0 && repb != 0) || (b != 0 && repb == 0) )
				continue;
			
			if( ax * repax < 0 )
				continue;
			
			if( ay * repay < 0 )
				continue;

			if( b * repb < 0 )
				continue;
			
			double factor = ax != 0 ? ax/repax : ay/repay;
			
			if( Math.abs(factor * repax - ax) < 1e-5 && Math.abs(factor * repay - ay) < 1e-5 && Math.abs(factor * repb - b) < 1e-5 )
				return;
		}
		
		fail("No contiene la restriccion (" + ax + ", " + ay + ", " + b + ")");
	}
}
