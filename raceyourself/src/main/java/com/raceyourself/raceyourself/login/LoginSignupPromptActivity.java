package com.raceyourself.raceyourself.login;

import java.util.Map;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.raceyourself.platform.models.Preference;
import com.raceyourself.platform.utils.Utils;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseActivity;
import com.viewpagerindicator.CirclePageIndicator;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EActivity(R.layout.activity_login_signup_prompt)
public class LoginSignupPromptActivity extends BaseActivity {

    public static final String PREFERENCE_SKIP_ONBOARDING = "skip_onboarding";

    private SectionsPagerAdapter sectionsPagerAdapter;

    @ViewById(R.id.titles)
    CirclePageIndicator titleIndicator;

    @ViewById(R.id.pager)
    ViewPager viewPager;

    @ViewById
    ImageView onboardingHill;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Boolean skipLogin = Preference.getBoolean(PREFERENCE_SKIP_ONBOARDING);

        if (skipLogin != null && skipLogin)
            signIn(null);
    }

    @AfterViews
    protected void afterViews() {
        sectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                log.info("Page selected, position is " + position);
                if(position == 4) {
                    titleIndicator.setVisibility(View.VISIBLE);
                } else if(position == 5) {
                    titleIndicator.setVisibility(View.INVISIBLE);
                }
            }
        });
        viewPager.setAdapter(sectionsPagerAdapter);

        titleIndicator.setFillColor(Color.parseColor("#ffffff"));
        titleIndicator.setRadius(20.0f);
        titleIndicator.setPageColor(Color.parseColor("#FF73BDC2"));
        titleIndicator.setStrokeColor(Color.parseColor("#00ffffff"));
        titleIndicator.setViewPager(viewPager);
        titleIndicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                log.info("page selected, position is " + position);
                if(position == 3) {
                    titleIndicator.setVisibility(View.VISIBLE);
                    onboardingHill.setVisibility(View.VISIBLE);
                } else if(position == 4) {
                    titleIndicator.setVisibility(View.INVISIBLE);
                    onboardingHill.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void signUp(View view) {
        String host = Utils.WS_URL;
        if (!host.endsWith("/"))
            host += "/";
        Uri uri = Uri.parse(host + "users/sign_up");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void signIn(View view) {
        Intent signIn = new Intent(this, LoginActivity.class);
        startActivity(signIn);
        finish(); // user can't come back here so activity can be safely destroyed.
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final Map<Integer,SlideFragment> items;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            int pageId = 0;
            Map<Integer,SlideFragment> temp = Maps.newHashMap();
            initSlideFragment(temp, R.layout.fragment_login_signup_prompt1, pageId++);
            initSlideFragment(temp, R.layout.fragment_login_signup_prompt2, pageId++);
            initSlideFragment(temp, R.layout.fragment_login_signup_prompt3, pageId++);
            initSlideFragment(temp, R.layout.fragment_login_signup_prompt4, pageId++);
            initSlideFragment(temp, R.layout.fragment_login_signup_prompt5, pageId++);
            initSlideFragment(temp, R.layout.fragment_login_signup_prompt6, pageId);

            items = ImmutableMap.copyOf(temp);
        }

        private void initSlideFragment(Map<Integer,SlideFragment> map, int layoutId, int pageId) {
            SlideFragment slideFragment = new SlideFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("layoutId", layoutId);
            slideFragment.setArguments(bundle);
            map.put(pageId, slideFragment);
        }

        @Override
        public Fragment getItem(int position) {
            return items.get(position);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Race Yourself (" + (position+1) + ")";
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SlideFragment extends Fragment {
        private int layoutId;

        public void setArguments(Bundle bundle) {
            layoutId = bundle.getInt("layoutId");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(layoutId, container, false);
        }
    }
}
