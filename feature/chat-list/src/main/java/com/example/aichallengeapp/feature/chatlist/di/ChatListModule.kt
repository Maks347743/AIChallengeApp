package com.example.aichallengeapp.feature.chatlist.di

import com.example.aichallengeapp.feature.chatlist.presentation.ChatListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatListModule = module {
    viewModel { ChatListViewModel(get(), get()) }
}
