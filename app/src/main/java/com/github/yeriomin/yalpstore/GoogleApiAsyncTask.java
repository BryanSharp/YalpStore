package com.github.yeriomin.yalpstore;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.yeriomin.playstoreapi.AuthException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

abstract class GoogleApiAsyncTask extends AsyncTask<String, Void, Throwable> {

    protected Context context;
    protected ProgressDialog progressDialog;
    protected TextView errorView;
    protected GoogleApiAsyncTask taskClone;
    private View progressIndicator;

    public void setProgressIndicator(View progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void prepareDialog(int messageId, int titleId) {
        this.progressDialog = Util.prepareProgressDialog(context, messageId, titleId);
    }

    public void setErrorView(TextView errorView) {
        this.errorView = errorView;
    }

    public void setTaskClone(GoogleApiAsyncTask taskClone) {
        this.taskClone = taskClone;
    }

    @Override
    protected void onPreExecute() {
        if (null != this.progressDialog) {
            this.progressDialog.show();
        }
        if (null != progressIndicator) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPostExecute(Throwable result) {
        if (null != this.progressDialog && ContextUtil.isAlive(context)) {
            this.progressDialog.dismiss();
        }
        if (null != progressIndicator) {
            progressIndicator.setVisibility(View.GONE);
        }
        Throwable e = result;
        if (result instanceof RuntimeException && null != result.getCause()) {
            e = result.getCause();
        }
        if (e != null) {
            processException(e);
        }
    }

    protected void processException(Throwable e) {
        Log.d(getClass().getName(), e.getClass().getName() + " caught during a google api request: " + e.getMessage());
        if (e instanceof AuthException) {
            processAuthException((AuthException) e);
        } else if (e instanceof IOException) {
            processIOException((IOException) e);
        } else {
            Log.e(getClass().getName(), "Unknown exception " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void processIOException(IOException e) {
        String message;
        if (noNetwork(e)) {
            message = this.context.getString(R.string.error_no_network);
        } else {
            message = this.context.getString(R.string.error_network_other, e.getClass().getName() + " " + e.getMessage());
        }
        if (null != this.errorView) {
            this.errorView.setText(message);
        } else {
            ContextUtil.toastLong(this.context, message);
        }
    }

    protected void processAuthException(AuthException e) {
        if (!ContextUtil.isAlive(context)) {
            Log.e(getClass().getName(), "AuthException happened and the provided context is not ui capable");
            return;
        }
        AccountTypeDialogBuilder builder = new AccountTypeDialogBuilder(this.context);
        builder.setTaskClone(this.taskClone);
        if (e instanceof CredentialsEmptyException) {
            Log.i(getClass().getName(), "Credentials empty");
            if (new FirstLaunchChecker(context).isFirstLogin()) {
                Log.i(getClass().getName(), "First launch, so using built-in account");
                builder.logInWithPredefinedAccount();
                ContextUtil.toast(context, R.string.first_login_message);
                return;
            }
        } else {
            ContextUtil.toast(this.context, R.string.error_incorrect_password);
            new PlayStoreApiAuthenticator(context).logout();
        }
        builder.show();
    }

    static public boolean noNetwork(Throwable e) {
        return e instanceof UnknownHostException
            || e instanceof SSLHandshakeException
            || e instanceof SSLPeerUnverifiedException
            || e instanceof ConnectException
            || e instanceof SocketException
            || e instanceof SocketTimeoutException;
    }
}
