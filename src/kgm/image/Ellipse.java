package kgm.image;

/**
 * An ellipse.
 * @author Kenton McHenry
 */
public class Ellipse
{
	public double x;
	public double y;
	public double xr;
	public double yr;
	public double theta;
	
	/**
	 * Class constructor.
	 * @param x the x-coordinate of the ellipse's center
	 * @param y the y-coordinate of the ellipse's center
	 * @param xr the radius in the x-direction
	 * @param yr the radius in the y-direction
	 * @param theta the rotation of the ellipse
	 */
	public Ellipse(double x, double y, double xr, double yr, double theta)
	{
		this.x = x;
		this.y = y;
		this.xr = xr;
		this.yr = yr;
		this.theta = theta;
	}
}