package testproj.ru.gpstest;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

public class AddGeofenceDialog extends DialogFragment {

    public interface AddGeofenceDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    AddGeofenceDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_makegeofence, null))
        .setTitle("Введите название точки")

        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.onDialogPositiveClick(AddGeofenceDialog.this);
            }
        })
        .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.onDialogNegativeClick(AddGeofenceDialog.this);
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mListener = (AddGeofenceDialogListener) context;
        }catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
            + "must implement AddGeofenceDialogListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            mListener = (AddGeofenceDialogListener) activity;
        }catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement AddGeofenceDialogListener");
        }
    }
}
