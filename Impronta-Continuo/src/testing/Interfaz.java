package testing;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import colgen.CplexCG;
import colgen.Dualizer;
import colgen.SolverCG;
import colgen.SolverCplex;
import colgen.HeuristicaInicial;
import continuo.Modelo;
import continuo.Separador;
import continuo.SeparadorCliqueHorizontal;
import continuo.SeparadorCliqueVertical;
import continuo.SeparadorGenCliqueHorizontal;
import continuo.SeparadorGenCliqueVertical;
import continuo.SolverContinuo;
import ilog.cplex.IloCplex;
import impronta.Discretizacion;
import impronta.Instancia;
import impronta.Pad;
import impronta.Restriccion;
import impronta.Solucion;

public class Interfaz
{
    public static void main(String[] args) throws Exception
    {
		System.out.println("UFO Continuo - 0.74");
		ArgMap argmap = new ArgMap(args);
		
		if( argmap.containsArg("-inst") == false )
		{
			System.out.println("  -model [s]        Modelo a utilizar [cont|colgen|cplex]");
			System.out.println("  -inst [f]         Instancia (f = file.xml, random.seed.s.obst.ssize.obstsize)");
			System.out.println("  -pads [n]         Pads a usar en el modelo");
			System.out.println("  -time [n]         Tiempo maximo en segundos");
			System.out.println("  -symm             Rompimiento de simetrías");
			System.out.println("  -cuts             Cortes dinámicos");
			System.out.println("  -cutpool          Pool de cortes");
			System.out.println("  -obj [cant|area]  Función objetivo");
			System.out.println("  -show             Muestra la solución");
		}
		
		if( !argmap.containsArg("-model") )
			return;
		
		Interfaz interfaz = new Interfaz(argmap.stringArg("-inst", "Instancias/pol.1s.07.xml"));

		if( argmap.stringArg("-model", "").equals("cont") )
			interfaz.resolverContinuo(argmap);
		
		if( argmap.stringArg("-model", "").equals("colgen") )
			interfaz.resolverRowCol(argmap);
		
		if( argmap.stringArg("-model", "").equals("cplex") )
			interfaz.resolverCplex();
    }
    
    public Interfaz(String archivo)
    {
    	_instancia = construirInstancia(archivo);
    }
    
    private Instancia _instancia;
    private SolverCG _solverCG;
    private SolverContinuo _solverContinuo;
    private SolverCplex _solverCplex;
    private Solucion _solucion;
    private JFrame _framePrincipal;
    private DrawingPanel _panelPrincipal;    
    private JFrame _frameDualizer;
    private DrawingPanel _panelDualizer;    

	private void resolverContinuo(ArgMap argmap)
	{
		long inicio = System.currentTimeMillis();

		Separador.setActivo(argmap.containsArg("-cuts"));

		_solverContinuo = new SolverContinuo(_instancia);
        _solverContinuo.setEliminacionSimetrias(argmap.containsArg("-symm"));
        _solverContinuo.setCutPool(argmap.containsArg("-cutpool"));
        _solverContinuo.setPads(argmap.intArg("-pads", 20));
        _solverContinuo.setObjetivo(argmap.stringArg("-obj", "cant").equals("cant") ? Modelo.Objetivo.Cantidad : Modelo.Objetivo.Area);
        _solverContinuo.setTiempoMaximo(argmap.doubleArg("-time", 60));

        _solucion = _solverContinuo.resolver();
        
        long fin = System.currentTimeMillis();
        System.out.println(" -> Tiempo total: " + (fin - inicio) / 1000.0 + " sg.\r\n");

        if( argmap.containsArg("-show") )
        {
	    	crearVentana();
	        mostrarInstancia(_panelPrincipal);
	        mostrarSolucion();
	        mostrar(_framePrincipal);
        }
        
        System.out.println();
        System.out.print(_instancia.getArchivo() + " | Cont. | ");
        System.out.print(_solverContinuo.getStatus() + " | ");
        System.out.print(String.format("%5.2f", (fin - inicio) / 1000.0) + " seg | ");
        System.out.print("Obj: " + String.format("%5.2f", _solverContinuo.getObjValue()) + " | ");
        System.out.print(String.format("%5.2f", _solverContinuo.getGap()) + " % | ");
        System.out.print("Nodes: " + _solverContinuo.getNodes() + " | ");
        System.out.print("Cuts: " + _solverContinuo.getUserCuts() + " / " + Separador.getActivaciones() + " | ");
        System.out.print("SH: " + SeparadorCliqueHorizontal.getResumen() + " | ");
        System.out.print("SV: " + SeparadorCliqueVertical.getResumen() + " | ");
        System.out.print("SGH: " + SeparadorGenCliqueHorizontal.getResumen() + " | ");
        System.out.print("SGV: " + SeparadorGenCliqueVertical.getResumen() + " | ");
        System.out.print(argmap);
        System.out.println();
	}
	
	private void resolverRowCol(ArgMap argmap)
	{
		SolverCG solver = new SolverCG(_instancia);
		solver.iniciar();
		solver.iterar();
		
        if( argmap.containsArg("-show") )
        {
	    	crearVentana();
	        mostrarInstancia(_panelPrincipal);
	       	mostrarRelajacion(solver.primales());
	       	mostrarRestricciones(solver.duales());
        	mostrar(_framePrincipal);
        	
        	crearDualizer();
        	mostrarInstancia(_panelDualizer);
        	mostrarSolucionDualizer(solver.getDualizer());
        	mostrar(_frameDualizer);
        }
	}
	
	private void resolverCplex()
	{
		if( _solverCG == null )
			return;
		
		Set<Pad> pads = _solverCG.getCplex().primales().keySet();
		Set<Point> points = _solverCG.getCplex().duales().keySet();
		
		_solverCplex = new SolverCplex(_instancia, pads, points);
		_solucion = _solverCplex.resolver();
	}

	private Instancia construirInstancia(String archivo)
	{
		if( archivo.contains("random") == false )
		{
	    	Instancia._formato = Instancia.Formato.French;
	    	Instancia._alinear = true;
			Instancia instancia = new Instancia(archivo);

			return instancia;
		}
		else
		{
			String[] campos = archivo.split("\\.");
			int seed = Integer.parseInt(campos[1]);
			int semillas = Integer.parseInt(campos[2]);
			int obstaculos = Integer.parseInt(campos[3]);
			int tamanoSemilla = Integer.parseInt(campos[4]);
			int tamanoObstaculos = Integer.parseInt(campos[5]);
			
			return new Generador(seed).generar(semillas, obstaculos, tamanoSemilla, tamanoObstaculos);
		}
	}

	private void mostrarInstancia(DrawingPanel panel)
	{
		if( panel == null || _instancia == null )
			return;
		
        for(Polygon envolvente: _instancia.getRegion().getEnvolventes())
        	panel.addGeometry(envolvente);
        
        for(Polygon agujero: _instancia.getRegion().getAgujeros())
        	panel.addGeometry(agujero);
        
        for(Restriccion restriccion: _instancia.getRestricciones())
        	panel.addGeometry(restriccion.getPolygon(), Color.BLACK, Color.LIGHT_GRAY, true);
	}
	
	@SuppressWarnings("unused")
	private void mostrarOGIP()
	{
        _panelPrincipal.addOGIP(_instancia.getOGIP());
	}
	
	private void mostrarDiscretizacion(Discretizacion discretizacion)
	{
		if( _panelPrincipal == null )
			return;
		
		System.out.println(discretizacion.getPuntos().getCoordinates().length + " puntos generados");
       	_panelPrincipal.addGeometry(discretizacion.getPuntos());
	}

	private void mostrarSolucion()
	{
		if( _panelPrincipal == null || _solucion == null )
			return;
		
        for(Pad pad: _solucion.getPads())
        {
            _panelPrincipal.addGeometry(pad.getPerimetro());
//            _panelPrincipal.addGeometry(pad.getLocacion(), Color.BLACK, null, true);
            _panelPrincipal.addGeometry(pad.getCentro(), Color.RED);
        }
        
     	DecimalFormat df = new DecimalFormat("##0.00");
        System.out.println(" -> Pads: " + _solucion.getPads().size());
        System.out.println(" -> Area cubierta: " + df.format(_solucion.porcentajeCubierto()) + " %");
	}

	@SuppressWarnings("unused")
	private void imprimirRelajacion()
	{
		if( _solverCG == null )
			return;
		
		Map<Pad, Double> solucion = _solverCG.primales();
        for(Pad pad: solucion.keySet())
        	System.out.println(" -> Pad (" + pad.getCentro() + ") = " + solucion.get(pad));
        
    	System.out.println(" -> " + _solverCG.getCplex().cantidadVariables() + " pads activos");
        System.out.println();
	}

	private void mostrarRelajacion(Map<Pad, Double> solucion)
	{
		if( _panelPrincipal == null )
			return;
		
		// Ordena los pads por los valores de sus variables
		ArrayList<Pad> pads = new ArrayList<Pad>(solucion.keySet());
		Collections.sort(pads, (i,j) -> (int)Math.signum(solucion.get(i) - solucion.get(j)));
		
        for(Pad pad: pads)
        {
        	int rgbNum = 255 - (int)(255.0 * solucion.get(pad));
        	Color color = new Color(rgbNum,rgbNum,rgbNum);

            _panelPrincipal.addGeometry(pad.getPerimetro(), color);
        }
	}

	private enum Duales { todos, positivos };
	
	@SuppressWarnings("unused")
	private void mostrarDuales(Map<Point, Double> duales, Duales modalidad)
	{
		if( _panelPrincipal == null )
			return;
		
    	double max = duales.values().stream().max(Double::compare).get();
    	
		// Ordena los puntos por los valores de sus variables duales
		ArrayList<Point> points = new ArrayList<Point>(duales.keySet());
		Collections.sort(points, (i,j) -> (int)Math.signum(duales.get(i) - duales.get(j)));
		
		for(Point point: points)
        {
        	if( duales.get(point) < 0.01 && modalidad == Duales.positivos )
        		continue;
        	
        	int rgbNum = (int)(255.0 * (max - duales.get(point)) / max);
        	Color color = new Color(rgbNum,rgbNum,rgbNum);
        	
            _panelPrincipal.addGeometry(point, color);
        }
	}

	@SuppressWarnings("unused")
	private void mostrarRestricciones(Map<Point, Double> duales)
	{
		if( _panelPrincipal == null )
			return;
		
        for(Point point: duales.keySet())
            _panelPrincipal.addGeometry(point, Color.BLUE);
	}

	@SuppressWarnings("unused")
	private void mostrarVioladorDual()
	{
		if( _panelPrincipal == null || _solverCG == null )
			return;
		
		Pad pad = _solverCG.violadorDual();
		
		if( pad != null )
			_panelPrincipal.addGeometry(pad.getPerimetro(), Color.RED);
	}

	@SuppressWarnings("unused")
	private void mostrarInputDualizer()
	{
		if( _panelDualizer == null || _solverCG == null )
			return;

        for(Pad pad: _solverCG.getDualizer().getPads())
        	_panelDualizer.addGeometry(pad.getPerimetro(), Color.WHITE);
        
        for(Point point: _solverCG.getDualizer().getPuntos())
        	_panelDualizer.addGeometry(point, Color.BLUE);
	}

	@SuppressWarnings("unused")
	private void mostrarPadNoFactible()
	{
		if( _panelDualizer == null || _solverCG == null || _solverCG.getDualizer().getPadNoFactible() == null )
			return;

       	_panelDualizer.addGeometry(_solverCG.getDualizer().getPadNoFactible().getPerimetro(), Color.RED);
	}
	
	@SuppressWarnings("unused")
	private void mostrarSolucionDualizer(Dualizer dualizer)
	{
		if( _panelDualizer == null )
			return;
		
		Map<Point, Double> vars = dualizer.getXVars();
    	double max = vars.values().stream().max(Double::compare).get();
    	System.out.println("Max dualizer var: " + max);
    	
        for(Point point: vars.keySet()) if( vars.get(point) > 0.01 )
        {
        	int rgbNum = (int)(255.0 * (max - vars.get(point)) / max);
        	Color color = new Color(rgbNum,rgbNum,rgbNum);
        	
            _panelDualizer.addGeometry(point, color);
            System.out.println("y(" + point + ") = " + vars.get(point));
            
            if( _instancia.getSemillas().size() == 1 )
            {
                _panelDualizer.addGeometry(Pad.rigido(_instancia, _instancia.getSemilla(0), point.getCoordinate()).getPerimetro(), color, color, false);
                _panelDualizer.addGeometry(Pad.rigido(_instancia, _instancia.getSemilla(0), point.getCoordinate()).getPerimetro(), Color.GRAY);
            }
        }
	}
	
	@SuppressWarnings("unused")
	private void imprimirDuales(Map<Point, Double> duales)
	{
		if( _solverCG == null )
			return;
		
        for(Point point: duales.keySet())
        	System.out.println(" -> Dual (" + point + ") = " + duales.get(point));
        
        System.out.println();
	}
	
	@SuppressWarnings("unused")
	private void imprimirResumen()
	{
		CplexCG cplex = _solverCG.getCplex();
		Dualizer dualizer = _solverCG.getDualizer();
		DecimalFormat format = new DecimalFormat("###0.00");
		
		System.out.print(" -> It " + _solverCG.getIteracion() + ": ");
		
		if( dualizer != null )
		{
			System.out.print("DS = " + (dualizer.getCplexStatus() == IloCplex.Status.Optimal ? "Opt" : "***"));
			System.out.print(", dobj = " + format.format(dualizer.getObjetivo()));
			System.out.print(_solverCG.esDualOptimo() ? " *" : "  ");
		}
		
		System.out.print(" - Primal: ");
		System.out.print(cplex.cantidadVariables() + " vars; ");
		System.out.print(cplex.cantidadRestricciones() + " constr; ");
		System.out.print("obj = " + format.format(cplex.funcionObjetivo()) + "; ");
		System.out.print(cplex.variablesFraccionarias() + " fract");
		System.out.print(_solverCG.esDualViolado() ? " (DV)" : "");
		System.out.print(_solverCG.esPrimalViolado() ? " (PV)" : "");
		System.out.println();
	}

	private void crearVentana()
	{
    	_framePrincipal = new JFrame("Solucion primal");
        _framePrincipal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        _framePrincipal.setSize(500, 500);
        
		_panelPrincipal = new DrawingPanel();
        _framePrincipal.add(_panelPrincipal);
	}
	
	@SuppressWarnings("unused")
	private void crearDualizer()
	{
		_frameDualizer = new JFrame("Dualizador");
	    _frameDualizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    _frameDualizer.setSize(500, 500);
	        
		_panelDualizer = new DrawingPanel();
		_frameDualizer.add(_panelDualizer);
	}

	private void mostrar(JFrame frame)
	{
		if( frame.isVisible() )
			frame.repaint();
		else
			frame.setVisible(true);
	}
	
	@SuppressWarnings("unused")
	private void limpiar(DrawingPanel panel)
	{
		if( panel != null )
			panel.clear();
	}
}
