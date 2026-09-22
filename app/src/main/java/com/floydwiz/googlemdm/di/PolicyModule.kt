package com.floydwiz.googlemdm.di

import com.floydwiz.googlemdm.enterprise.command.CommandExecutor
import com.floydwiz.googlemdm.enterprise.command.DeviceCommandExecutor
import com.floydwiz.googlemdm.enterprise.policy.DevicePolicyApplier
import com.floydwiz.googlemdm.enterprise.policy.PolicyApplier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PolicyModule {

    @Binds
    @Singleton
    abstract fun bindPolicyApplier(impl: DevicePolicyApplier): PolicyApplier

    @Binds
    @Singleton
    abstract fun bindCommandExecutor(impl: DeviceCommandExecutor): CommandExecutor
}
