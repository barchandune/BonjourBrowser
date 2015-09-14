/*
 * Copyright (C) 2015 Andriy Druk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.druk.bonjour.browser.ui.fragment;

import com.druk.bonjour.browser.R;
import com.druk.bonjour.browser.databinding.ServiceItemBinding;
import com.druk.bonjour.browser.dnssd.BonjourService;
import com.druk.bonjour.browser.dnssd.RxDNSSD;
import com.druk.bonjour.browser.ui.ServiceActivity;
import com.druk.bonjour.browser.ui.adapter.ServiceAdapter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class ServiceBrowserFragment extends Fragment {

    private static final String KEY_REG_TYPE = "reg_type";
    private static final String KEY_DOMAIN = "domain";

    protected Subscription mSubscription;
    protected ServiceAdapter mAdapter;
    protected String mReqType;
    protected String mDomain;

    protected RecyclerView mRecyclerView;

    public static Fragment newInstance(String domain, String regType) {
        return fillArguments(new ServiceBrowserFragment(), domain, regType);
    }

    protected static Fragment fillArguments(Fragment fragment, String domain, String regType) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_DOMAIN, domain);
        bundle.putString(KEY_REG_TYPE, regType);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mReqType = getArguments().getString(KEY_REG_TYPE);
            mDomain = getArguments().getString(KEY_DOMAIN);
        }
        mAdapter = new ServiceAdapter<>(getContext(), ViewHolder::new, v -> {
            if (mRecyclerView != null) {
                int position = mRecyclerView.getLayoutManager().getPosition(v);
                ServiceActivity.startActivity(v.getContext(), mAdapter.getItem(position));
            }
        });
        mAdapter.setHasStableIds(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_service_browser, container, false);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mRecyclerView.getContext()));
        mRecyclerView.setAdapter(mAdapter);
        return mRecyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDiscovery();
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
    }

    protected void startDiscovery() {
        mSubscription = RxDNSSD.queryRecords(RxDNSSD.resolve(RxDNSSD.browse(mReqType, mDomain)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bonjourService -> {
                    if ((bonjourService.flags & BonjourService.DELETED_MASK) != BonjourService.DELETED_MASK) {
                        mAdapter.add(bonjourService);
                    } else {
                        mAdapter.remove(bonjourService);
                    }
                    mAdapter.notifyDataSetChanged();
                }, throwable -> {
                    Log.e("DNSSD", "Error: ", throwable);
                });
    }

    protected void stopDiscovery() {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }

    public static class ViewHolder extends ServiceAdapter.ViewHolder{

        private final ServiceItemBinding mBinding;

        public ViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.service_item, parent, false));
            mBinding = ServiceItemBinding.bind(itemView);
        }

        @Override
        public void setService(BonjourService service) {
            mBinding.setService(service);
        }
    }
}