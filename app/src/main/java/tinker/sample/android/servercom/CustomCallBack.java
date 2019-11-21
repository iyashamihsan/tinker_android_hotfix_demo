package tinker.sample.android.servercom;

import android.app.ProgressDialog;
import android.content.Context;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomCallBack<T> implements Callback<T> {
    ProgressDialog progressDialog;

    public CustomCallBack(Context context) {
        this.progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        hideProgress();
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        hideProgress();
    }

    private void hideProgress(){

        if (progressDialog.isShowing()){
            progressDialog.dismiss();
        }
    }
}
