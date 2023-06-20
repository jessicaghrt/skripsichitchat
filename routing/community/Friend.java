package routing.community;

/**
 * A helper class for the community package that stores a start and end value
 * for some abstract duration. Generally, in this package, the duration being
 * stored is a time duration.
 * 
 * @author PJ Dillon, University of Pittsburgh
 */
public class Friend
{
	/** The start value */
	public double rank;
	
	/** The end value */
	public int sum;
	
	/**
	 * Standard constructor that assigns s to start and e to end.
	 * 
	 * @param s Initial start value
	 * @param e Initial end value
	 */
	public Friend(double r, int s) {rank = r; sum = s;}
}
