package com.raceyourself.raceyourself.login;

import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.raceyourself.raceyourself.R;

public class LoginSignupPrompt extends Activity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup_prompt);

        Button signInBtn = (Button) findViewById(R.id.signin_prompt_btn);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

    }

    public void signUp(View view) {
        Uri uri = Uri.parse("http://auth.raceyourself.com/users/sign_up");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void signIn(View view) {
        Intent signIn = new Intent(this, LoginActivity.class);
        startActivity(signIn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login_signup_prompt, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            LoginSlideFragment fragment = null;
            switch(position + 1) {
                case 1:
                    fragment = new FirstSlideFragment();
                    break;

                case 2:
                    fragment = new SecondSlideFragment();
                    break;

                case 3:
                    fragment = new ThirdSlideFragment();
                    break;

                case 4:
                    fragment = new FourthSlideFragment();
                    break;

                case 5:
                    fragment = new FifthSlideFragment();
                    break;

                case 6:
                    fragment = new SixthSlideFragment();
                    break;

                default:
                    fragment = new FirstSlideFragment();
                    break;
            }

            return LoginSlideFragment.newInstance(position + 1, fragment);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                case 3:
                    return getString(R.string.title_section4).toUpperCase(l);
                case 4:
                    return getString(R.string.title_section5).toUpperCase(l);
                case 5:
                    return getString(R.string.title_section6).toUpperCase(l);
            }
            return null;
        }
    }

    public static class LoginSlideFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";

        private static int pageNumber;

        public static LoginSlideFragment newInstance(int sectionNumber, LoginSlideFragment fragment) {
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            pageNumber = sectionNumber;
            fragment.setArguments(args);

            return fragment;
        }

        public LoginSlideFragment() {}

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class FirstSlideFragment extends LoginSlideFragment {

        public FirstSlideFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_login_signup_prompt1, container, false);

            return rootView;
        }
    }

    public static class SecondSlideFragment extends LoginSlideFragment {

        public SecondSlideFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_login_signup_prompt2, container, false);

            return rootView;
        }
    }

    public static class ThirdSlideFragment extends LoginSlideFragment {

        public ThirdSlideFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_login_signup_prompt3, container, false);

            return rootView;
        }
    }

    public static class FourthSlideFragment extends LoginSlideFragment {
        public FourthSlideFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_login_signup_prompt4, container, false);

            return rootView;
        }
    }

    public static class FifthSlideFragment extends LoginSlideFragment {
        public FifthSlideFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_login_signup_prompt5, container, false);

            return rootView;
        }
    }

    public static class SixthSlideFragment extends LoginSlideFragment {
        public SixthSlideFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_login_signup_prompt6, container, false);

            return rootView;
        }
    }

}
