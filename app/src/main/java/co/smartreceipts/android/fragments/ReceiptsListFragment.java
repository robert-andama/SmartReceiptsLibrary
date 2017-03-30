package co.smartreceipts.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.common.base.Preconditions;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import co.smartreceipts.android.R;
import co.smartreceipts.android.activities.Attachable;
import co.smartreceipts.android.activities.FragmentProvider;
import co.smartreceipts.android.activities.NavigationHandler;
import co.smartreceipts.android.adapters.ReceiptCardAdapter;
import co.smartreceipts.android.analytics.events.Events;
import co.smartreceipts.android.imports.ActivityFileResultImporter;
import co.smartreceipts.android.imports.ActivityFileResultImporterResponse;
import co.smartreceipts.android.imports.AttachmentSendFileImporter;
import co.smartreceipts.android.imports.CameraInteractionController;
import co.smartreceipts.android.imports.RequestCodes;
import co.smartreceipts.android.model.Attachment;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.model.factory.ReceiptBuilderFactory;
import co.smartreceipts.android.persistence.PersistenceManager;
import co.smartreceipts.android.persistence.database.controllers.ReceiptTableEventsListener;
import co.smartreceipts.android.persistence.database.controllers.impl.ReceiptTableController;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.utils.log.Logger;
import dagger.android.support.AndroidSupportInjection;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import wb.android.dialog.BetterDialogBuilder;
import wb.android.flex.Flex;

public class ReceiptsListFragment extends ReceiptsFragment implements ReceiptTableEventsListener {

    // Outstate
    private static final String OUT_HIGHLIGHTED_RECEIPT = "out_highlighted_receipt";
    private static final String OUT_IMAGE_URI = "out_image_uri";

    @Inject
    Flex flex;

    @Inject
    PersistenceManager persistenceManager;

    private ReceiptTableController receiptTableController;
    private ReceiptCardAdapter adapter;
    private ActivityFileResultImporter activityFileResultImporter;
    private Receipt highlightedReceipt;
    private Uri imageUri;
    private ProgressBar loadingProgress;
    private ProgressBar updatingDataProgress;
    private TextView noDataAlert;
    private Attachable attachable;

    private FloatingActionMenu floatingActionMenu;
    private View floatingActionMenuActiveMaskView;

    private NavigationHandler navigationHandler;
    private CompositeSubscription compositeSubscription;


    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);

        super.onAttach(context);
        if (context instanceof Attachable) {
            attachable = (Attachable) context;
        } else {
            throw new ClassCastException("The ReceiptFragment's Activity must extend the Attachable interfaces");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.debug(this, "onCreate");
        receiptTableController = getSmartReceiptsApplication().getTableControllerManager().getReceiptTableController();
        adapter = new ReceiptCardAdapter(getActivity(), persistenceManager.getPreferenceManager(),
                getSmartReceiptsApplication().getBackupProvidersManager());
        navigationHandler = new NavigationHandler(getActivity(), new FragmentProvider());
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(OUT_IMAGE_URI);
            highlightedReceipt = savedInstanceState.getParcelable(OUT_HIGHLIGHTED_RECEIPT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.debug(this, "onCreateView");
        final View rootView = inflater.inflate(getLayoutId(), container, false);
        loadingProgress = (ProgressBar) rootView.findViewById(R.id.progress);
        updatingDataProgress = (ProgressBar) rootView.findViewById(R.id.progress_adding_new);
        noDataAlert = (TextView) rootView.findViewById(R.id.no_data);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int id = v.getId();
                if (id == R.id.receipt_action_camera) {
                    getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.AddPictureReceipt);
                    addPictureReceipt();
                } else if (id == R.id.receipt_action_text) {
                    getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.AddTextReceipt);
                    addTextReceipt();
                } else if (id == R.id.receipt_action_import) {
                    getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ImportPictureReceipt);
                    importReceipt();
                }
            }
        };
        rootView.findViewById(R.id.receipt_action_camera).setOnClickListener(listener);
        rootView.findViewById(R.id.receipt_action_text).setOnClickListener(listener);
        rootView.findViewById(R.id.receipt_action_import).setOnClickListener(listener);
        rootView.findViewById(R.id.receipt_action_text).setVisibility(getConfigurationManager().isTextReceiptsOptionAvailable() ? View.VISIBLE : View.GONE);
        floatingActionMenu = (FloatingActionMenu) rootView.findViewById(R.id.fab_menu);
        floatingActionMenuActiveMaskView = rootView.findViewById(R.id.fab_active_mask);
        floatingActionMenuActiveMaskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intentional stub to block click events when this view is active
            }
        });
        floatingActionMenu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean isOpen) {
                // TODO: Animate this change with the buttons appearing for cleaner effect
                final Context context = floatingActionMenuActiveMaskView.getContext();
                if (isOpen) {
                    floatingActionMenuActiveMaskView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.out_from_bottom_right));
                    floatingActionMenuActiveMaskView.setVisibility(View.VISIBLE);
                } else {
                    floatingActionMenuActiveMaskView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.in_to_bottom_right));
                    floatingActionMenuActiveMaskView.setVisibility(View.GONE);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Logger.debug(this, "onActivityCreated");
        mTrip = ((ReportInfoFragment) getParentFragment()).getTrip();
        Preconditions.checkNotNull(mTrip, "A valid trip is required");
        activityFileResultImporter = new ActivityFileResultImporter(getActivity(), getFragmentManager(),
                mTrip, persistenceManager, getSmartReceiptsApplication().getAnalyticsManager(),
                getSmartReceiptsApplication().getOcrInteractor());
        setListAdapter(adapter); // Set this here to ensure this has been laid out already
    }

    public int getLayoutId() {
        return R.layout.receipt_fragment_layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.debug(this, "onResume");
        compositeSubscription = new CompositeSubscription();
        receiptTableController.subscribe(this);
        receiptTableController.get(mTrip);
        compositeSubscription.add(activityFileResultImporter.getResultStream()
                .subscribe(new Action1<ActivityFileResultImporterResponse>() {
                    @Override
                    public void call(ActivityFileResultImporterResponse response) {
                        Logger.info(ReceiptsListFragment.this, "Successfully handled the import of {}", response);
                        switch (response.getRequestCode()) {
                            case RequestCodes.IMPORT_GALLERY_IMAGE:
                            case RequestCodes.IMPORT_GALLERY_PDF:
                            case RequestCodes.NATIVE_NEW_RECEIPT_CAMERA_REQUEST:
                                navigationHandler.navigateToCreateNewReceiptFragment(mTrip, response.getFile(), response.getOcrResponse());
                                break;
                            case RequestCodes.NATIVE_ADD_PHOTO_CAMERA_REQUEST:
                                final Receipt updatedReceipt = new ReceiptBuilderFactory(highlightedReceipt).setImage(response.getFile()).build();
                                receiptTableController.update(highlightedReceipt, updatedReceipt, new DatabaseOperationMetadata());
                                break;
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(getActivity(), getFlexString(R.string.IMG_SAVE_ERROR), Toast.LENGTH_SHORT).show();
                        highlightedReceipt = null;
                        updatingDataProgress.setVisibility(View.GONE);
                        activityFileResultImporter.dispose();
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        highlightedReceipt = null;
                        updatingDataProgress.setVisibility(View.GONE);
                        activityFileResultImporter.dispose();
                    }
                }));
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getView() != null && isVisibleToUser) {
            // Refresh as soon as we're visible
            receiptTableController.get(mTrip);
        }
    }

    @Override
    public void onPause() {
        Logger.debug(this, "onPause");
        floatingActionMenu.close(false);
        receiptTableController.unsubscribe(this);
        compositeSubscription.unsubscribe();
        compositeSubscription = null;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Logger.debug(this, "onSaveInstanceState");
        outState.putParcelable(OUT_IMAGE_URI, imageUri);
        outState.putParcelable(OUT_HIGHLIGHTED_RECEIPT, highlightedReceipt);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Logger.debug(this, "onActivityResult");
        Logger.debug(this, "Result Code: {}", resultCode);
        Logger.debug(this, "Request Code: {}", requestCode);

        // Need to make this call here, since users with "Don't keep activities" will hit this call
        // before any of onCreate/onStart/onResume is called. This should restore our current trip (what
        // onResume() would normally do to prevent a variety of crashes that we might encounter
        if (mTrip == null) {
            mTrip = ((ReportInfoFragment) getParentFragment()).getTrip();
        }

        // Null out the last request
        final Uri cachedImageSaveLocation = imageUri;
        imageUri = null;

        updatingDataProgress.setVisibility(View.VISIBLE);
        activityFileResultImporter.onActivityResult(requestCode, resultCode, data, cachedImageSaveLocation);

        if (resultCode != Activity.RESULT_OK) {
            updatingDataProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public final void addPictureReceipt() {
        imageUri = new CameraInteractionController(this).takePhoto();
    }

    public final void addTextReceipt() {
        navigationHandler.navigateToCreateNewReceiptFragment(mTrip, null, null);
    }

    private void importReceipt() {
        final ImportPhotoPdfDialogFragment fragment = new ImportPhotoPdfDialogFragment();
        fragment.show(getFragmentManager(), ImportPhotoPdfDialogFragment.TAG);
    }

    public final boolean showReceiptMenu(final Receipt receipt) {
        highlightedReceipt = receipt;
        final BetterDialogBuilder builder = new BetterDialogBuilder(getActivity());
        builder.setTitle(receipt.getName()).setCancelable(true).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        final Attachment attachment = attachable.getAttachment();
        if (attachment != null && attachment.isDirectlyAttachable()) {
            final String[] receiptActions;
            final int attachmentStringId = attachment.isPDF() ? R.string.pdf : R.string.image;
            final int receiptStringId = receipt.hasPDF() ? R.string.pdf : R.string.image;
            final String attachFile = getString(R.string.action_send_attach, getString(attachmentStringId));
            final String viewFile = getString(R.string.action_send_view, getString(receiptStringId));
            final String replaceFile = getString(R.string.action_send_replace, getString(receipt.hasPDF() ? R.string.pdf : R.string.image));
            if (receipt.hasFile()) {
                receiptActions = new String[]{viewFile, replaceFile};
            } else {
                receiptActions = new String[]{attachFile};
            }
            builder.setItems(receiptActions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    final String selection = receiptActions[item];
                    if (selection != null) {
                        if (selection.equals(viewFile)) { // Show File
                            if (attachment.isPDF()) {
                                ReceiptsListFragment.this.showPDF(receipt);
                            } else {
                                ReceiptsListFragment.this.showImage(receipt);
                            }
                        } else if (selection.equals(attachFile) || selection.equals(replaceFile)) { // Attach File to Receipt
                            // TODO: Make this more graceful
                            final AttachmentSendFileImporter importer = new AttachmentSendFileImporter(getActivity(),
                                    mTrip, persistenceManager, receiptTableController,
                                    getSmartReceiptsApplication().getAnalyticsManager());
                            compositeSubscription.add(importer.importAttachment(attachment, receipt)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Action1<File>() {
                                        @Override
                                        public void call(File file) {
                                            // Intentional no-op
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            Toast.makeText(getActivity(), R.string.database_error, Toast.LENGTH_SHORT).show();
                                        }
                                    }));
                        }
                    }
                }
            });
        } else {
            final String receiptActionEdit = getString(R.string.receipt_dialog_action_edit);
            final String receiptActionView = getString(R.string.receipt_dialog_action_view, getString(receipt.hasPDF() ? R.string.pdf : R.string.image));
            final String receiptActionCamera = getString(R.string.receipt_dialog_action_camera);
            final String receiptActionDelete = getString(R.string.receipt_dialog_action_delete);
            final String receiptActionMoveCopy = getString(R.string.receipt_dialog_action_move_copy);
            final String receiptActionSwapUp = getString(R.string.receipt_dialog_action_swap_up);
            final String receiptActionSwapDown = getString(R.string.receipt_dialog_action_swap_down);
            final String[] receiptActions;
            if (!receipt.hasFile()) {
                receiptActions = new String[]{receiptActionEdit, receiptActionCamera, receiptActionDelete, receiptActionMoveCopy, receiptActionSwapUp, receiptActionSwapDown};
            } else {
                receiptActions = new String[]{receiptActionEdit, receiptActionView, receiptActionDelete, receiptActionMoveCopy, receiptActionSwapUp, receiptActionSwapDown};
            }
            builder.setItems(receiptActions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    final String selection = receiptActions[item];
                    if (selection != null) {
                        if (selection.equals(receiptActionEdit)) { // Edit Receipt
                            getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuEdit);
                            // ReceiptsListFragment.this.receiptMenu(trip, receipt, null);
                            navigationHandler.navigateToEditReceiptFragment(mTrip, receipt);
                        } else if (selection.equals(receiptActionCamera)) { // Take Photo
                            getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuRetakePhoto);
                            imageUri = new CameraInteractionController(ReceiptsListFragment.this).addPhoto();
                        } else if (selection.equals(receiptActionView)) { // View Photo/PDF
                            if (receipt.hasPDF()) {
                                getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuViewImage);
                                ReceiptsListFragment.this.showPDF(receipt);
                            } else {
                                getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuViewPdf);
                                ReceiptsListFragment.this.showImage(receipt);
                            }
                        } else if (selection.equals(receiptActionDelete)) { // Delete Receipt
                            getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuDelete);
                            ReceiptsListFragment.this.deleteReceipt(receipt);
                        } else if (selection.equals(receiptActionMoveCopy)) {// Move-Copy
                            getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuMoveCopy);
                            ReceiptMoveCopyDialogFragment.newInstance(receipt).show(getFragmentManager(), ReceiptMoveCopyDialogFragment.TAG);
                        } else if (selection.equals(receiptActionSwapUp)) { // Swap Up
                            getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuSwapUp);
                            receiptTableController.swapUp(receipt);
                        } else if (selection.equals(receiptActionSwapDown)) { // Swap Down
                            getSmartReceiptsApplication().getAnalyticsManager().record(Events.Receipts.ReceiptMenuSwapDown);
                            receiptTableController.swapDown(receipt);
                        }
                    }
                    dialog.cancel();
                }
            });
        }
        builder.show();
        return true;
    }

    private void showImage(@NonNull Receipt receipt) {
        navigationHandler.navigateToViewReceiptImage(receipt);
    }

    private void showPDF(@NonNull Receipt receipt) {
        navigationHandler.navigateToViewReceiptPdf(receipt);
    }

    public final void deleteReceipt(final Receipt receipt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.delete_item, receipt.getName())).setMessage(R.string.delete_sync_information).setCancelable(true).setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                receiptTableController.delete(receipt, new DatabaseOperationMetadata());
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        }).show();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showReceiptMenu(adapter.getItem(position));
    }

    @Override
    public void onGetSuccess(@NonNull List<Receipt> receipts, @NonNull Trip trip) {
        if (isAdded()) {
            loadingProgress.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
            if (receipts.isEmpty()) {
                noDataAlert.setVisibility(View.VISIBLE);
            } else {
                noDataAlert.setVisibility(View.INVISIBLE);
            }
            adapter.notifyDataSetChanged(receipts);
            updateActionBarTitle(getUserVisibleHint());
        }
    }

    @Override
    public void onGetFailure(@Nullable Throwable e, @NonNull Trip trip) {
        Toast.makeText(getActivity(), R.string.database_get_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGetSuccess(@NonNull List<Receipt> list) {
        // TODO: Respond?
    }

    @Override
    public void onGetFailure(@Nullable Throwable e) {
        Toast.makeText(getActivity(), R.string.database_get_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInsertSuccess(@NonNull Receipt receipt, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isResumed()) {
            receiptTableController.get(mTrip);
        }
    }

    @Override
    public void onInsertFailure(@NonNull Receipt receipt, @Nullable Throwable e, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            Toast.makeText(getActivity(), getFlexString(R.string.database_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdateSuccess(@NonNull Receipt oldReceipt, @NonNull Receipt newReceipt, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            if (newReceipt.getFile() != null && newReceipt.getFileLastModifiedTime() != oldReceipt.getFileLastModifiedTime()) {
                final int stringId;
                if (oldReceipt.getFile() != null) {
                    if (newReceipt.hasImage()) {
                        stringId = R.string.toast_receipt_image_replaced;
                    } else {
                        stringId = R.string.toast_receipt_pdf_replaced;
                    }
                } else {
                    if (newReceipt.hasImage()) {
                        stringId = R.string.toast_receipt_image_added;
                    } else {
                        stringId = R.string.toast_receipt_pdf_added;
                    }
                }
                Toast.makeText(getActivity(), getString(stringId, newReceipt.getName()), Toast.LENGTH_SHORT).show();
                final Attachment attachment = attachable.getAttachment();
                if (attachment != null && attachment.isDirectlyAttachable()) {
                    attachable.setAttachment(null);
                    getActivity().finish();
                }
            }

            receiptTableController.get(mTrip);
            ReceiptsListFragment.this.updateActionBarTitle(getUserVisibleHint());
        }
    }

    @Override
    public void onUpdateFailure(@NonNull Receipt oldReceipt, @Nullable Throwable e, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            Toast.makeText(getActivity(), getFlexString(R.string.database_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteSuccess(@NonNull Receipt receipt, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            receiptTableController.get(mTrip);
            ReceiptsListFragment.this.updateActionBarTitle(getUserVisibleHint());
        }
    }

    @Override
    public void onDeleteFailure(@NonNull Receipt receipt, @Nullable Throwable e, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            Toast.makeText(getActivity(), getFlexString(R.string.database_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopySuccess(@NonNull Receipt oldReceipt, @NonNull Receipt newReceipt) {
        if (isAdded()) {
            receiptTableController.get(mTrip);
            Toast.makeText(getActivity(), getFlexString(R.string.toast_receipt_copy), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopyFailure(@NonNull Receipt oldReceipt, @Nullable Throwable e) {
        if (isAdded()) {
            Toast.makeText(getActivity(), getFlexString(R.string.COPY_ERROR), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSwapSuccess() {
        receiptTableController.get(mTrip);
    }

    @Override
    public void onSwapFailure(@Nullable Throwable e) {
        // TODO: Respond?
    }

    @Override
    public void onMoveSuccess(@NonNull Receipt oldReceipt, @NonNull Receipt newReceipt) {
        if (isAdded()) {
            receiptTableController.get(mTrip);
            Toast.makeText(getActivity(), getFlexString(R.string.toast_receipt_move), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMoveFailure(@NonNull Receipt oldReceipt, @Nullable Throwable e) {
        if (isAdded()) {
            Toast.makeText(getActivity(), getFlexString(R.string.MOVE_ERROR), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFlexString(int id) {
        return getFlexString(flex, id);
    }

}