package com.veniosg.dir.mvvm.model.storage.operation;

import com.veniosg.dir.android.ui.toast.ToastFactory;
import com.veniosg.dir.mvvm.model.storage.access.StorageAccessManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;

import static com.veniosg.dir.mvvm.model.storage.operation.FakeStorageAccessManager.aFakeStorageAccessManager;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FileOperationRunnerTest {
    private final File target = new File("");
    private final FakeArguments fakeArgs = new FakeArguments(target);

    @Mock
    private StorageAccessManager mockStorageAccessManager;
    @Mock
    private ToastFactory mockToastFactory;
    @Mock
    private FileOperation<FakeArguments> mockOperation;
    private FileOperationRunner runner;
    private final FakeStorageAccessManager fakeStorageAccessManager = aFakeStorageAccessManager();

    @Before
    public void setUp() {
        initMocks(this);
        runner = new FileOperationRunner(mockStorageAccessManager, mockToastFactory);
    }

    @Test
    public void normalSuccess_operatesOnlyNormal() {
        givenOperateSucceeds();

        whenRunnerRuns();

        verify(mockOperation).onStartOperation(fakeArgs);
        verify(mockOperation).operate(fakeArgs);
        verify(mockOperation).onResult(true, fakeArgs);
        // Make sure we don't query collaborators unnecessarily as they can be heavy
        verifyNoMoreInteractions(mockOperation);
        verifyZeroInteractions(mockStorageAccessManager);
        verifyZeroInteractions(mockToastFactory);
    }

    @Test
    public void normalFailure_noNeedToWrite_operatesOnlyNormal() {
        givenOperateFails();
        givenOperationDoesNotNeedWriteAccess();

        whenRunnerRuns();

        verify(mockOperation).onStartOperation(fakeArgs);
        verify(mockOperation).operate(fakeArgs);
        verify(mockOperation).needsWriteAccess();
        verify(mockOperation).onResult(false, fakeArgs);
        verifyNoMoreInteractions(mockOperation);
        verifyZeroInteractions(mockStorageAccessManager);
        verifyZeroInteractions(mockToastFactory);
    }

    @Test
    public void normalFailure_needsToWrite_hasNoWriteAccess_operatesOnlyNormalAndRequestsAccess() {
        givenOperateFails();
        givenOperationNeedsWriteAccess();
        givenNoStorageWriteAccess();

        whenRunnerRuns();

        verify(mockOperation).onStartOperation(fakeArgs);
        verify(mockOperation).operate(fakeArgs);
        verify(mockOperation).needsWriteAccess();
        verify(mockStorageAccessManager).hasWriteAccess(target);
        verify(mockOperation).onRequestingAccess();
        verify(mockStorageAccessManager).requestWriteAccess(any(), any());
        verifyNoMoreInteractions(mockOperation);
        verifyNoMoreInteractions(mockStorageAccessManager);
        verifyZeroInteractions(mockToastFactory);
    }

    @Test
    public void normalFailure_needsToWrite_hasWriteAccess_isNotSafBased_operatesOnlyNormal() {
        givenOperateFails();
        givenOperationNeedsWriteAccess();
        givenStorageWriteAccess();
        givenDeviceNotSafBased();

        whenRunnerRuns();

        verify(mockOperation).onStartOperation(fakeArgs);
        verify(mockOperation).operate(fakeArgs);
        verify(mockOperation).needsWriteAccess();
        verify(mockStorageAccessManager).hasWriteAccess(target);
        verify(mockStorageAccessManager).isSafBased();
        verify(mockOperation).onResult(false, fakeArgs);
        verifyNoMoreInteractions(mockOperation);
        verifyNoMoreInteractions(mockStorageAccessManager);
        verifyZeroInteractions(mockToastFactory);
    }

    @Test
    public void normalFailure_needsToWrite_hasWriteAccess_isSafBased_operatesSaf() {
        givenOperateFails();
        givenOperationNeedsWriteAccess();
        givenStorageWriteAccess();
        givenDeviceSafBased();
        givenOperateSafFails();

        whenRunnerRuns();

        verify(mockOperation).onStartOperation(fakeArgs);
        verify(mockOperation).operate(fakeArgs);
        verify(mockOperation).needsWriteAccess();
        verify(mockStorageAccessManager).hasWriteAccess(target);
        verify(mockStorageAccessManager).isSafBased();
        verify(mockOperation).operateSaf(fakeArgs);
        verify(mockOperation).onResult(false, fakeArgs);
        verifyNoMoreInteractions(mockOperation);
        verifyNoMoreInteractions(mockStorageAccessManager);
        verifyZeroInteractions(mockToastFactory);
    }

    @Test
    public void normalFailure_needsToWrite_hasWriteAccess_isSafBased_returnsSafResult() {
        givenOperateFails();
        givenOperationNeedsWriteAccess();
        givenStorageWriteAccess();
        givenDeviceSafBased();
        givenOperateSafSucceeds();

        whenRunnerRuns();

        verify(mockOperation).onResult(true, fakeArgs);
        // Rest of interactions are verified above
    }

    @Test
    public void onRequestWriteAccess_callBackWhenDenied() {
        runner = new FileOperationRunner(fakeStorageAccessManager, mockToastFactory);
        givenOperateFails();
        givenOperationNeedsWriteAccess();
        givenNoStorageWriteAccess();
        givenWriteAccessGetsDenied();

        whenRunnerRuns();

        verify(mockOperation).onStartOperation(fakeArgs);
        verify(mockOperation).operate(fakeArgs);
        verify(mockOperation).onRequestingAccess();
        verify(mockOperation).onAccessDenied();
        verify(mockOperation, never()).onResult(anyBoolean(), any());
        verifyZeroInteractions(mockToastFactory);
    }

    @Test
    @Ignore
    public void onRequestWriteAccess_trySafWhenGranted() {
        runner = new FileOperationRunner(fakeStorageAccessManager, mockToastFactory);
        givenOperateFails();
        givenOperationNeedsWriteAccess();
        givenNoStorageWriteAccess();
        givenWriteAccessGetsGranted();

        whenRunnerRuns();

        // TODO SDCARD Make this verify in order.....?
        verify(mockOperation, times(2)).onStartOperation(fakeArgs);
        verify(mockOperation, times(2)).operate(fakeArgs);
        verify(mockOperation).onRequestingAccess();
        verify(mockOperation).operateSaf(fakeArgs);
        verify(mockOperation).onResult(anyBoolean(), any());
        verifyZeroInteractions(mockToastFactory);
    }

    @Test
    @Ignore
    public void onRequestWriteAccess_runAgainAndToastOnError() {
        runner = new FileOperationRunner(fakeStorageAccessManager, mockToastFactory);
        givenOperateFails();
        givenOperationNeedsWriteAccess();
        givenNoStorageWriteAccess();
        givenWriteAccessGetsDenied();

        whenRunnerRuns();

        // TODO SDCARD Make this verify in order.....
        verify(mockOperation, times(2)).onStartOperation(fakeArgs);
        verify(mockOperation, times(2)).operate(fakeArgs);
        verify(mockOperation).onRequestingAccess();
        verify(mockToastFactory).grantAccessWrongDirectory();
        verify(mockOperation).operateSaf(fakeArgs);
        verify(mockOperation).onResult(anyBoolean(), any());
    }

    private void givenOperateFails() {
        when(mockOperation.operate(any())).thenReturn(false);
    }

    private void givenOperateSucceeds() {
        when(mockOperation.operate(any())).thenReturn(true);
    }

    private void givenOperationDoesNotNeedWriteAccess() {
        when(mockOperation.needsWriteAccess()).thenReturn(false);
    }

    private void givenOperationNeedsWriteAccess() {
        when(mockOperation.needsWriteAccess()).thenReturn(true);
    }

    private void givenStorageWriteAccess() {
        when(mockStorageAccessManager.hasWriteAccess(any())).thenReturn(true);
        fakeStorageAccessManager.thatHasWriteAccess();
    }

    private void givenNoStorageWriteAccess() {
        when(mockStorageAccessManager.hasWriteAccess(any())).thenReturn(false);
        fakeStorageAccessManager.thatHasNoWriteAccess();
    }

    private void givenWriteAccessGetsGranted() {
        fakeStorageAccessManager.thatAlwaysGrantsAccess();
    }

    private void givenWriteAccessGetsDenied() {
        fakeStorageAccessManager.thatAlwaysDeniesAccess();
    }

    private void givenWriteAccessRequestProducesError() {
        fakeStorageAccessManager.thatAlwaysErrors();
    }

    private void givenDeviceSafBased() {
        when(mockStorageAccessManager.isSafBased()).thenReturn(true);
    }

    private void givenDeviceNotSafBased() {
        when(mockStorageAccessManager.isSafBased()).thenReturn(false);
    }

    private void givenOperateSafFails() {
        when(mockOperation.operateSaf(any())).thenReturn(false);
    }

    private void givenOperateSafSucceeds() {
        when(mockOperation.operateSaf(any())).thenReturn(true);
    }

    private void whenRunnerRuns() {
        runner.run(mockOperation, fakeArgs);
    }
}