package com.example.scfappv1;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.scfappv1.databinding.ActivityBottomNavBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
public class BottomNav extends AppCompatActivity {

    private ActivityBottomNavBinding binding;
    private FirebaseAuth mAuth;
    public FirebaseUser currentUser;
    public NavController navController;
    public BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        binding = ActivityBottomNavBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_bottom_nav);
        //TextView textView = (TextView) findViewById(R.id.dirText);
        //textView.setText("Test");
        NavigationUI.setupWithNavController(binding.navView, navController);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        //Handle item switch
        if (item.getItemId() == R.id.logout){
            mAuth.signOut();
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.account_menu){
            //Check to make sure we are not already on that page
            int id = navController.getCurrentDestination().getId();
            if (id != R.id.navigation_account) {
                //Changing fragment to the account fragment
                navController.navigate(R.id.navigation_account);
                //Disabling the bottom bar so the user cannot leave the page until the fragment has closed
                enableBottomBar(false);
            }
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //Function for disabling the bottom nav bar
    private void enableBottomBar(boolean enable){
        for (int i = 0; i < navView.getMenu().size(); i++) {
            navView.getMenu().getItem(i).setEnabled(enable);
        }
    }

    //Adding functionality to the back button to enable to bottom nav bar when pressed
    @Override
    public void onBackPressed() {
        enableBottomBar(true);
        finish();
    }
}