package co.smartreceipts.android.widget;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.transitionseverywhere.Slide;
import com.transitionseverywhere.TransitionManager;

import co.smartreceipts.android.R;

public class Tooltip extends RelativeLayout {

    private Button mButtonNo, mButtonYes;
    private TextView mMessageText;
    private ImageView mCloseIcon, mErrorIcon;

    public Tooltip(Context context) {
        super(context);
        init();
    }

    public Tooltip(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Tooltip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.tooltip, this);
        mMessageText = (TextView) findViewById(R.id.tooltip_message);
        mButtonNo = (Button) findViewById(R.id.tooltip_no);
        mButtonYes = (Button) findViewById(R.id.tooltip_yes);
        mCloseIcon = (ImageView) findViewById(R.id.tooltip_close_icon);
        mErrorIcon = (ImageView) findViewById(R.id.tooltip_error_icon);

        setVisibility(VISIBLE);
    }

    public void setError(int messageStringId, OnClickListener closeClickListener) {
        setViewStateError();

        mMessageText.setText(getContext().getText(messageStringId));
        mCloseIcon.setOnClickListener(closeClickListener);
    }

    public void setErrorWithoutClose(int messageStringId, OnClickListener tooltipClickListener) {
        setViewStateError();
        mCloseIcon.setVisibility(GONE);

        mMessageText.setText(getContext().getText(messageStringId));
        setOnClickListener(tooltipClickListener);
    }

    public void setInfo(int infoStringId, OnClickListener tooltipClickListener, OnClickListener closeClickListener) {
        setInfoBackground();

        mMessageText.setVisibility(VISIBLE);
        mCloseIcon.setVisibility(VISIBLE);

        mErrorIcon.setVisibility(GONE);
        mButtonNo.setVisibility(GONE);
        mButtonYes.setVisibility(GONE);

        mMessageText.setText(getContext().getText(infoStringId));
        setOnClickListener(tooltipClickListener);
        mCloseIcon.setOnClickListener(closeClickListener);
    }

    public void setQuestion(int questionStringId, OnClickListener noClickListener, OnClickListener yesClickListener) {
        setInfoBackground();

        mMessageText.setVisibility(VISIBLE);
        mButtonNo.setVisibility(VISIBLE);
        mButtonYes.setVisibility(VISIBLE);

        mCloseIcon.setVisibility(GONE);
        mErrorIcon.setVisibility(GONE);

        mMessageText.setText(getContext().getText(questionStringId));
        mButtonNo.setOnClickListener(noClickListener);
        mButtonYes.setOnClickListener(yesClickListener);
    }

    private void setErrorBackground() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.smart_receipts_colorError));
    }

    private void setInfoBackground() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.smart_receipts_colorAccent));
    }

    private void setViewStateError() {
        setErrorBackground();

        mMessageText.setVisibility(VISIBLE);
        mCloseIcon.setVisibility(VISIBLE);
        mErrorIcon.setVisibility(VISIBLE);

        mButtonNo.setVisibility(GONE);
        mButtonYes.setVisibility(GONE);
    }

    public void hideWithAnimation() {
        TransitionManager.beginDelayedTransition((ViewGroup) getParent(), new Slide(Gravity.TOP));
        setVisibility(GONE);
    }

    public void showWithAnimation() {
        TransitionManager.beginDelayedTransition((ViewGroup) getParent(), new Slide(Gravity.TOP));
        setVisibility(VISIBLE);
    }
}
