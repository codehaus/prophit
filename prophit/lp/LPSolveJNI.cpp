
#include <jni.h>
#include "lp_solve_LPLibrary.h"
#include "auto_array_ptr.h"
// Need to include this first or lpkit messes it up
#include <math.h>
#include "lpkit.h"

lprec* toLPRec(jlong hLP)
{
	return reinterpret_cast<lprec*>(hLP);
}

REAL* allocateReal(JNIEnv* pEnv, jdoubleArray jArray)
{
	jsize len = pEnv->GetArrayLength(jArray);
	REAL* data = new REAL[len];
	if ( data == NULL )
	{
		jclass jException = pEnv->FindClass("L/java/lang/OutOfMemoryError");
		char buf[256];
		sprintf(buf, "Unable to allocate REAL[] of size %d", len);
		pEnv->ThrowNew(jException, buf);
	}
	return data;
}

void copyDoubleArray(JNIEnv* pEnv, REAL* row, jdoubleArray jArray)
{
	jsize len = pEnv->GetArrayLength(jArray);
	jdouble* pjArray = pEnv->GetDoubleArrayElements(jArray, NULL);
	for ( jsize i = 0; i < len; ++i )
	{
		row[i] = pjArray[i];
	}
	pEnv->ReleaseDoubleArrayElements(jArray, pjArray, JNI_ABORT);
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    make_lp
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL Java_lp_solve_LPLibrary_make_1lp
  (JNIEnv *pEnv, jobject, jint rows, jint columns)
{
	return reinterpret_cast<jlong>(make_lp(rows, columns));
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    delete_lp
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_delete_1lp
  (JNIEnv *, jobject, jlong hLP)
{
	lprec* lp = toLPRec(hLP);
	delete_lp(lp);
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    set_obj_fn
 * Signature: (J[D)V
 */
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_set_1obj_1fn
  (JNIEnv *pEnv, jobject, jlong hLP, jdoubleArray jRow)
{
	auto_array_ptr<REAL> row(allocateReal(pEnv, jRow));
	if ( row.get() == NULL )
		return;

	copyDoubleArray(pEnv, row.get(), jRow);

	set_obj_fn(toLPRec(hLP), row.get());
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    add_constraint
 * Signature: (J[DSD)V
 */
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_add_1constraint
  (JNIEnv *pEnv, jobject, jlong hLP, jdoubleArray jRow, jshort constr_type, jdouble rh)
{
	auto_array_ptr<REAL> row(allocateReal(pEnv, jRow));
	if ( row.get() == NULL )
		return;

	copyDoubleArray(pEnv, row.get(), jRow);

	add_constraint(toLPRec(hLP), row.get(), constr_type, rh);
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    set_maxim
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_set_1maxim
  (JNIEnv *, jobject, jlong hLP)
{
	set_maxim(toLPRec(hLP));
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    set_minim
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_set_1minim
  (JNIEnv *, jobject, jlong hLP)
{
	set_minim(toLPRec(hLP));
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    solve
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_lp_solve_LPLibrary_solve
  (JNIEnv *, jobject, jlong hLP)
{
	return solve(toLPRec(hLP));
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    get_solution_value
 * Signature: (JI)D
 */
JNIEXPORT jdouble JNICALL Java_lp_solve_LPLibrary_get_1solution_1value
  (JNIEnv *, jobject, jlong hLP, jint column)
{
	return get_solution_value(toLPRec(hLP), column);
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    print_lp
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_print_1lp
  (JNIEnv *, jobject, jlong hLP)
{
	print_lp(toLPRec(hLP));
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    print_solution
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_print_1solution
  (JNIEnv *, jobject, jlong hLP)
{
	print_solution(toLPRec(hLP));
}

/*
 * Class:     lp_solve_LPLibrary
 * Method:    get_row
 * Signature: (JI[D)V
 */
/*
JNIEXPORT void JNICALL Java_lp_solve_LPLibrary_get_1row
  (JNIEnv *pEnv, jobject, jlong hLP, jint row_nr, jdoubleArray jRowData)
{
	auto_array_ptr<REAL> row(allocateReal(pEnv, jRowData));
	if ( row.get() == NULL )
		return;

	get_row(toLPRec(hLP), row_nr, row.get());

	jsize len = pEnv->GetArrayLength(jRowData);
	jdouble* pjArray = pEnv->GetDoubleArrayElements(jRowData, NULL);
	for ( jsize i = 0; i < len; ++i )
	{
		pjArray[i] = row[i];
	}
	pEnv->ReleaseDoubleArrayElements(jRowData, pjArray, 0);
}
*/