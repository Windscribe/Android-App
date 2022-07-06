/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

class State {
    enum class FavouriteState(var stateValue: Int) {
        NotFavourite(1), Favourite(2);
    }

    enum class MenuButtonState(var state: Int) {
        NotSelected(1), Selected(2);
    }

    enum class TwoState {
        NOT_SELECTED, SELECTED
    }
}