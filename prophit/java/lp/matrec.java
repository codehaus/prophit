/* $Header$ */
/* $Log$
 * Revision 1.1  2002/11/21 19:56:01  sfrancis
 * Added LP solver from http://www.cs.wustl.edu/~javagrp/help/LinearProgramming.html
 *
# Revision 1.2  1996/06/06  19:47:20  hma
# added package statement
#
# Revision 1.1  1996/05/21  02:04:15  hma
# Initial revision
# */

package lp;

public class matrec
{
  int row_nr;
  double value;
  public matrec(int r, double v) {
    row_nr = r;
    value = v;
  }
}
