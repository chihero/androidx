package com.mysdk

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat.Companion.create
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import kotlin.Int

public abstract class AbstractSandboxedSdkProvider : SandboxedSdkProviderCompat() {
  public override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
    val sdk = createBackwardsCompatibleSdk(context!!)
    return create(BackwardsCompatibleSdkStubDelegate(sdk))
  }

  public override fun getView(
    windowContext: Context,
    params: Bundle,
    width: Int,
    height: Int,
  ): View {
    TODO("Implement")
  }

  protected abstract fun createBackwardsCompatibleSdk(context: Context): BackwardsCompatibleSdk
}
