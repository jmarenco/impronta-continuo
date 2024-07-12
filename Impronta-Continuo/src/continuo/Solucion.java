package continuo;

import java.text.DecimalFormat;

import impronta.Instancia;

public class Solucion
{
	private Modelo _modelo;
	private Instancia _instancia;
	
	private double[] x;
	private double[] y;
	private double[][] w;
	private double[][] l;
	private double[][] r;
	private double[][] t;
	private double[][] b;
	
	public Solucion(Modelo modelo, Separador padre)
	{
		_modelo = modelo;
		_instancia = modelo.getInstancia();
		
		int pads = modelo.getPads();
		int semillas = _instancia.getSemillas().size();
		
		x = new double[pads];
		y = new double[pads];
		w = new double[pads][semillas];
		l = new double[pads][pads];
		r = new double[pads][pads];
		t = new double[pads][pads];
		b = new double[pads][pads];
		
		try
		{
			for(int i=0; i<pads; ++i)
			{
				x[i] = padre.getValor( _modelo.getx(i) );
				y[i] = padre.getValor( _modelo.gety(i) );
			
				for(int k=0; k<semillas; ++k)
					w[i][k] = padre.getValor( _modelo.getw(i, k) );
			
				for(int j=i+1; j<pads; ++j)
				{
					l[i][j] = padre.getValor( _modelo.getl(i, j) );
					t[i][j] = padre.getValor( _modelo.gett(i, j) );
					
					if( _modelo.permiteSimetrias() )
					{
						r[i][j] = padre.getValor( _modelo.getr(i, j) );
						b[i][j] = padre.getValor( _modelo.getb(i, j) );
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public double getx(int i)
	{
		return x[i];
	}
	public double gety(int i)
	{
		return y[i];
	}
	public double getw(int i, int k)
	{
		return w[i][k];
	}
	public double getl(int i, int j)
	{
		return l[i][j];
	}
	public double getr(int i, int j)
	{
		return r[i][j];
	}
	public double gett(int i, int j)
	{
		return t[i][j];
	}
	public double getb(int i, int j)
	{
		return b[i][j];
	}
	
	@Override public String toString()
	{
		DecimalFormat formato = new DecimalFormat("##0.00");
		String ret = "";
		
		int pads = _modelo.getPads();
		for(int i=0; i<pads; ++i)
			ret += "Pad " + i + ": (" + formato.format(getx(i)) + ", " + formato.format(gety(i)) + ") - w[" + i + ",0] = " + getw(i,0) + "\n";

		for(int i=0; i<pads; ++i)
		for(int j=i+1; j<pads; ++j)
			ret += "l[" + i + "," + j + "] = " + getl(i,j) + " - t[" + i + "," + j + "] = " + gett(i,j) + "\n";
		
		return ret;
	}
}
