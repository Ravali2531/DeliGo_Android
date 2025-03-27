package com.example.deligoandroid.Admin;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.deligoandroid.R;
import androidx.appcompat.widget.Toolbar;

public class ChatManagementActivity extends AppCompatActivity {
    private static final String TAG = "ChatManagementActivity";
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_chat_management);

            // Initialize views with null checks
            toolbar = findViewById(R.id.toolbar);
            if (toolbar == null) {
                Log.e(TAG, "Toolbar not found in layout");
                return;
            }

            viewPager = findViewById(R.id.viewPager);
            if (viewPager == null) {
                Log.e(TAG, "ViewPager not found in layout");
                return;
            }

            tabLayout = findViewById(R.id.tabLayout);
            if (tabLayout == null) {
                Log.e(TAG, "TabLayout not found in layout");
                return;
            }

            // Set up toolbar
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Chat Management");
            }

            // Set up ViewPager with fragments
            ChatPagerAdapter pagerAdapter = new ChatPagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);

            // Connect TabLayout with ViewPager2
            new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Customer Support");
                            break;
                        case 1:
                            tab.setText("Driver Support");
                            break;
                        case 2:
                            tab.setText("Restaurant Support");
                            break;
                    }
                }
            ).attach();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private static class ChatPagerAdapter extends FragmentStateAdapter {
        public ChatPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new CustomerSupportFragment();
                case 1:
                    return new DriverSupportFragment();
                case 2:
                    return new RestaurantSupportFragment();
                default:
                    return new CustomerSupportFragment();
            }
        }
    }
} 