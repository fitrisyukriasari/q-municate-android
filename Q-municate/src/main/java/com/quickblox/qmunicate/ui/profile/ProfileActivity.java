package com.quickblox.qmunicate.ui.profile;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.module.content.model.QBFile;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.qmunicate.App;
import com.quickblox.qmunicate.R;
import com.quickblox.qmunicate.core.command.Command;
import com.quickblox.qmunicate.qb.QBGetFileCommand;
import com.quickblox.qmunicate.qb.QBUpdateUserCommand;
import com.quickblox.qmunicate.service.QBServiceConsts;
import com.quickblox.qmunicate.ui.base.BaseActivity;
import com.quickblox.qmunicate.ui.uihelper.SimpleActionModeCallback;
import com.quickblox.qmunicate.ui.uihelper.SimpleTextWatcher;
import com.quickblox.qmunicate.ui.utils.GetImageFileTask;
import com.quickblox.qmunicate.ui.utils.GettingImageFileListener;
import com.quickblox.qmunicate.ui.utils.ImageHelper;

import java.io.File;
import java.io.IOException;

public class ProfileActivity extends BaseActivity implements GettingImageFileListener {
    private ImageView avatarImageView;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText statusMessageEditText;

    private ImageHelper imageHelper;
    private Bitmap avatarBitmapCurrent;
    private String fullnameCurrent;
    private String emailCurrent;
    private Bitmap avatarOldBitmap;
    private String fullnameOld;
    private String emailOld;
    private QBUser qbUser;
    private boolean isNeedUpdateAvatar;
    private Object actionMode;
    private boolean closeActionMode;

    public static void start(Context context) {
        Intent intent = new Intent(context, ProfileActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        useDoubleBackPressed = false;

        initUI();
        qbUser = App.getInstance().getUser();
        imageHelper = new ImageHelper(this);

        addAction(QBServiceConsts.UPDATE_USER_SUCCESS_ACTION, new UpdateUserSuccessAction());
        addAction(QBServiceConsts.UPDATE_USER_FAIL_ACTION, new FailAction(this));
        addAction(QBServiceConsts.GET_FILE_SUCCESS_ACTION, new GetFileSuccessAction());
        addAction(QBServiceConsts.GET_FILE_FAIL_ACTION, new FailAction(this));
        updateBroadcastActionList();

        initUsersData();
        initTextChangedListeners();
    }

    private void initUI() {
        avatarImageView = _findViewById(R.id.avatarImageView);
        fullNameEditText = _findViewById(R.id.fullNameEditText);
        emailEditText = _findViewById(R.id.emailEditText);
        statusMessageEditText = _findViewById(R.id.statusMessageEditText);
    }

    private void initTextChangedListeners() {
        TextWatcher textWatcherListener = new TextWatcherListener();
        fullNameEditText.addTextChangedListener(textWatcherListener);
        emailEditText.addTextChangedListener(textWatcherListener);
    }

    private void initChangingEditText(EditText editText) {
        editText.setEnabled(true);
        editText.requestFocus();
    }

    private void initUsersData() {
        if (qbUser.getFileId() != null) {
            QBGetFileCommand.start(this, qbUser.getFileId());
        }
        fullNameEditText.setText(qbUser.getFullName());
        emailEditText.setText(qbUser.getEmail());

        avatarOldBitmap = ((BitmapDrawable) avatarImageView.getDrawable()).getBitmap();
        fullnameOld = fullNameEditText.getText().toString();
        emailOld = emailEditText.getText().toString();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (actionMode != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            closeActionMode = true;
            ((ActionMode) actionMode).finish();
            return true;
        } else {
            closeActionMode = false;
        }
        return super.dispatchKeyEvent(event);
    }

    public void changeAvatarOnClick(View view) {
        imageHelper.getImage();
    }

    public void changeFullNameOnClick(View view) {
        initChangingEditText(fullNameEditText);
    }

    public void changeEmailOnClick(View view) {
        initChangingEditText(emailEditText);
    }

    private void startAction() {
        if (actionMode != null) {
            return;
        }
        actionMode = startActionMode(new ActionModeCallback());
    }

    private void updateCurrentUserData() {
        avatarBitmapCurrent = ((BitmapDrawable) avatarImageView.getDrawable()).getBitmap();
        fullnameCurrent = fullNameEditText.getText().toString();
        emailCurrent = emailEditText.getText().toString();
    }

    private void updateUserData() {
        if (isUserDataChanges(fullnameCurrent, emailCurrent)) {
            try {
                saveChanges(avatarBitmapCurrent, fullnameCurrent, emailCurrent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateToParent();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            isNeedUpdateAvatar = true;
            avatarOldBitmap = ((BitmapDrawable) avatarImageView.getDrawable()).getBitmap();
            Uri originalUri = data.getData();
            avatarImageView.setImageURI(originalUri);
            startAction();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveChanges(final Bitmap avatar, final String fullname, final String email) throws IOException {
        if (isUserDataChanges(fullname, email)) {
            showProgress();
            qbUser.setFullName(fullname);
            qbUser.setEmail(email);

            if (isAvatarChanges(avatar) && isNeedUpdateAvatar) {
                new GetImageFileTask(this).execute(imageHelper, avatarImageView);
            } else {
                QBUpdateUserCommand.start(this, qbUser, null);
            }
        }
    }

    private boolean isAvatarChanges(Bitmap avatar) {
        return !imageHelper.equalsBitmaps(avatarOldBitmap, avatar);
    }

    private boolean isUserDataChanges(String fullname, String email) {
        return isNeedUpdateAvatar || !fullname.equals(fullnameOld) || !email.equals(emailOld);
    }

    @Override
    public void onGotImageFile(File imageFile) {
        QBUpdateUserCommand.start(this, qbUser, imageFile);
    }

    private class TextWatcherListener extends SimpleTextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            startAction();
        }
    }

    private class ActionModeCallback extends SimpleActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (!closeActionMode) {
                updateCurrentUserData();
                updateUserData();
            }
            actionMode = null;
        }
    }

    private class UpdateUserSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            QBUser user = (QBUser) bundle.getSerializable(QBServiceConsts.EXTRA_USER);
            App.getInstance().setUser(user);
            hideProgress();
        }
    }

    private class GetFileSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            QBFile file = (QBFile) bundle.getSerializable(QBServiceConsts.EXTRA_FILE);
            ImageLoader.getInstance().displayImage(file.getPublicUrl(), avatarImageView);
        }
    }
}