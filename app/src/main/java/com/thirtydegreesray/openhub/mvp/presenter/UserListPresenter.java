/*
 *    Copyright 2017 ThirtyDegreesRay
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.common.Event;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IUserListContract;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.SearchResult;
import com.thirtydegreesray.openhub.mvp.model.User;
import com.thirtydegreesray.openhub.ui.fragment.UserListFragment;
import com.thirtydegreesray.openhub.util.StringUtils;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * Created by ThirtyDegreesRay on 2017/8/16 17:38:43
 */

public class UserListPresenter extends BasePresenter<IUserListContract.View>
        implements IUserListContract.Presenter {

    @AutoAccess UserListFragment.UserListType type;
    @AutoAccess String user;
    @AutoAccess String repo;

    @AutoAccess SearchModel searchModel;

    private ArrayList<User> users;

    @Inject
    public UserListPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        if (type.equals(UserListFragment.UserListType.SEARCH)) {
            setEventSubscriber(true);
        }
        loadUsers(1, false);
    }

    @Override
    public void loadUsers(final int page, final boolean isReload) {
        if (type.equals(UserListFragment.UserListType.SEARCH)) {
            searchUsers(page);
            return;
        }
        mView.showLoading();
        final boolean readCacheFirst = page == 1 && !isReload;
        HttpObserver<ArrayList<User>> httpObserver =
                new HttpObserver<ArrayList<User>>() {
                    @Override
                    public void onError(Throwable error) {
                        mView.hideLoading();
                        handleError(error);
                    }

                    @Override
                    public void onSuccess(HttpResponse<ArrayList<User>> response) {
                        mView.hideLoading();
                        if (isReload || users == null || readCacheFirst) {
                            users = response.body();
                        } else {
                            users.addAll(response.body());
                        }
                        if(response.body().size() == 0 && users.size() != 0){
                            mView.setCanLoadMore(false);
                        } else {
                            mView.showUsers(users);
                        }
                    }
                };
        generalRxHttpExecute(new IObservableCreator<ArrayList<User>>() {
            @Override
            public Observable<Response<ArrayList<User>>> createObservable(boolean forceNetWork) {
                if (type.equals(UserListFragment.UserListType.STARGAZERS)) {
                    return getRepoService().getStargazers(forceNetWork, user, repo, page);
                } else if (type.equals(UserListFragment.UserListType.WATCHERS)) {
                    return getRepoService().getWatchers(forceNetWork, user, repo, page);
                } else if (type.equals(UserListFragment.UserListType.FOLLOWERS)) {
                    return getUserService().getFollowers(forceNetWork, user, page);
                } else if (type.equals(UserListFragment.UserListType.FOLLOWING)) {
                    return getUserService().getFollowing(forceNetWork, user, page);
                } else if (type.equals(UserListFragment.UserListType.ORG_MEMBERS)) {
                    return getUserService().getOrgMembers(forceNetWork, user, page);
                } else {
                    throw new IllegalArgumentException(type.name());
                }
            }
        }, httpObserver, readCacheFirst);
    }

    private void searchUsers(final int page) {
        mView.showLoading();
        HttpObserver<SearchResult<User>> httpObserver =
                new HttpObserver<SearchResult<User>>() {
                    @Override
                    public void onError(Throwable error) {
                        mView.hideLoading();
                        handleError(error);
                    }

                    @Override
                    public void onSuccess(HttpResponse<SearchResult<User>> response) {
                        mView.hideLoading();
                        if (users == null || page == 1) {
                            users = response.body().getItems();
                        } else {
                            users.addAll(response.body().getItems());
                        }
                        if(response.body().getItems().size() == 0 && users.size() != 0){
                            mView.setCanLoadMore(false);
                        } else {
                            mView.showUsers(users);
                        }
                    }
                };
        generalRxHttpExecute(new IObservableCreator<SearchResult<User>>() {
            @Override
            public Observable<Response<SearchResult<User>>> createObservable(boolean forceNetWork) {
                return getSearchService().searchUsers(searchModel.getQuery(), searchModel.getSort(),
                        searchModel.getOrder(), page);
            }
        }, httpObserver);
    }

    @Subscribe
    public void onSearchEvent(Event.SearchEvent searchEvent) {
        if (!searchEvent.searchModel.getType().equals(SearchModel.SearchType.User)) return;
        this.searchModel = searchEvent.searchModel;
        searchUsers(1);
    }

    private void handleError(Throwable error){
        if(!StringUtils.isBlankList(users)){
            mView.showErrorToast(getErrorTip(error));
        } else if(error instanceof HttpPageNoFoundError){
            mView.showUsers(new ArrayList<User>());
        }else{
            mView.showLoadError(getErrorTip(error));
        }
    }

}
