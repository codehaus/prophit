package lp.solve;

class LPLibrary
{
	{
		System.loadLibrary("LPSolveJNI");
	}

	public native long make_lp(int rows, int columns);

	public native void delete_lp(long hLP);

	public native void set_obj_fn(long hLP, double[] row);

	public native void add_constraint(long hLP, double[] row, short constr_type, double rh);

	public native void set_maxim(long hLP);

	public native void set_minim(long hLP);

	public native int solve(long hLP);

	public native double get_solution_value(long hLP, int column);

	public native void print_lp(long hLP);

	public native void print_solution(long hLP);
}
