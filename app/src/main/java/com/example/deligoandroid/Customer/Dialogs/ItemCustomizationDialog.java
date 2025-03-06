package com.example.deligoandroid.Customer.Dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Customer.Models.*;
import com.example.deligoandroid.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;
import java.util.*;

public class ItemCustomizationDialog extends BottomSheetDialogFragment {
    private MenuItem menuItem;
    private int quantity = 1;
    private double totalPrice;
    private Map<String, List<CustomizationSelection>> selectedCustomizations = new HashMap<>();
    private TextView quantityText;
    private TextView totalPriceText;
    private OnAddToCartListener onAddToCartListener;

    public static ItemCustomizationDialog newInstance(MenuItem menuItem) {
        ItemCustomizationDialog dialog = new ItemCustomizationDialog();
        Bundle args = new Bundle();
        args.putSerializable("menuItem", menuItem);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnAddToCartListener(OnAddToCartListener listener) {
        this.onAddToCartListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);
        
        if (getArguments() != null) {
            menuItem = (MenuItem) getArguments().getSerializable("menuItem");
            Log.d("ItemCustomizationDialog", "MenuItem received: " + menuItem.getName());
            Log.d("ItemCustomizationDialog", "Has customizations: " + menuItem.hasCustomizations());
            if (menuItem.getCustomizationOptions() != null) {
                Log.d("ItemCustomizationDialog", "Number of options: " + menuItem.getCustomizationOptions().size());
                for (CustomizationOption option : menuItem.getCustomizationOptions()) {
                    Log.d("ItemCustomizationDialog", "Option: " + option.getName() + ", Items: " + option.getOptions().size());
                }
            } else {
                Log.d("ItemCustomizationDialog", "Customization options is null");
            }
            totalPrice = menuItem.getPrice();
        } else {
            Log.e("ItemCustomizationDialog", "No arguments received");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getBehavior().setSkipCollapsed(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_item_customization, container, false);
        
        // Initialize views
        TextView itemName = view.findViewById(R.id.itemName);
        TextView itemDescription = view.findViewById(R.id.itemDescription);
        TextView itemPrice = view.findViewById(R.id.itemPrice);
        ImageButton decreaseQuantity = view.findViewById(R.id.decreaseQuantity);
        ImageButton increaseQuantity = view.findViewById(R.id.increaseQuantity);
        quantityText = view.findViewById(R.id.quantityText);
        totalPriceText = view.findViewById(R.id.totalPrice);
        Button addToCartButton = view.findViewById(R.id.addToCartButton);
        View cancelButton = view.findViewById(R.id.cancelButton);

        // Set basic item info
        itemName.setText(menuItem.getName());
        itemDescription.setText(menuItem.getDescription());
        itemPrice.setText(String.format("$%.2f", menuItem.getPrice()));
        updateTotalPrice();

        // Setup quantity controls
        decreaseQuantity.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                quantityText.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });

        increaseQuantity.setOnClickListener(v -> {
            quantity++;
            quantityText.setText(String.valueOf(quantity));
            updateTotalPrice();
        });

        // Setup customization options
        ViewGroup customizationsContainer = view.findViewById(R.id.customizationsContainer);
        setupCustomizationOptions(customizationsContainer);

        // Setup buttons
        addToCartButton.setOnClickListener(v -> {
            CartItem cartItem = createCartItem();
            if (onAddToCartListener != null) {
                onAddToCartListener.onAddToCart(cartItem);
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());

        return view;
    }

    private void setupCustomizationOptions(ViewGroup container) {
        if (menuItem == null) {
            Log.e("ItemCustomizationDialog", "MenuItem is null");
            return;
        }

        Log.d("ItemCustomizationDialog", "MenuItem details:");
        Log.d("ItemCustomizationDialog", "- Name: " + menuItem.getName());
        Log.d("ItemCustomizationDialog", "- HasCustomizations: " + menuItem.hasCustomizations());
        
        if (menuItem.getCustomizationOptions() == null) {
            Log.e("ItemCustomizationDialog", "Customization options is null");
            return;
        }

        Log.d("ItemCustomizationDialog", "Setting up " + menuItem.getCustomizationOptions().size() + " options");
        for (CustomizationOption option : menuItem.getCustomizationOptions()) {
            Log.d("ItemCustomizationDialog", "Processing option: " + option.getName());
            Log.d("ItemCustomizationDialog", "- Type: " + option.getType());
            Log.d("ItemCustomizationDialog", "- Required: " + option.isRequired());
            Log.d("ItemCustomizationDialog", "- Items count: " + option.getOptions().size());
            
            View optionView = getLayoutInflater().inflate(R.layout.item_customization_group, container, false);
            
            TextView titleText = optionView.findViewById(R.id.optionTitle);
            TextView requiredText = optionView.findViewById(R.id.requiredText);
            ViewGroup optionsContainer = optionView.findViewById(R.id.optionsContainer);

            titleText.setText(option.getName());
            if (option.isRequired()) {
                requiredText.setVisibility(View.VISIBLE);
            }

            Log.d("ItemCustomizationDialog", "Setting up option: " + option.getName() + ", Type: " + option.getType());
            if (option.getType().equals("single")) {
                setupRadioGroup(optionsContainer, option);
            } else {
                setupCheckboxGroup(optionsContainer, option);
            }

            container.addView(optionView);
        }
    }

    private void setupRadioGroup(ViewGroup container, CustomizationOption option) {
        RadioGroup radioGroup = new RadioGroup(getContext());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        for (CustomizationItem item : option.getOptions()) {
            RadioButton radioButton = (RadioButton) getLayoutInflater().inflate(
                R.layout.item_customization_radio, radioGroup, false);
            radioButton.setText(item.getName());
            if (item.getPrice() > 0) {
                radioButton.setText(String.format("%s (+$%.2f)", item.getName(), item.getPrice()));
            }
            radioGroup.addView(radioButton);

            radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    List<CustomizationSelection> selections = new ArrayList<>();
                    CustomizationSelection selection = new CustomizationSelection();
                    selection.setOptionId(option.getId());
                    selection.setOptionName(option.getName());
                    
                    SelectedItem selectedItem = new SelectedItem();
                    selectedItem.setId(item.getId());
                    selectedItem.setName(item.getName());
                    selectedItem.setPrice(item.getPrice());
                    
                    selection.setSelectedItems(Collections.singletonList(selectedItem));
                    selections.add(selection);
                    selectedCustomizations.put(option.getId(), selections);
                    updateTotalPrice();
                }
            });
        }

        container.addView(radioGroup);
    }

    private void setupCheckboxGroup(ViewGroup container, CustomizationOption option) {
        for (CustomizationItem item : option.getOptions()) {
            CheckBox checkBox = (CheckBox) getLayoutInflater().inflate(
                R.layout.item_customization_checkbox, container, false);
            checkBox.setText(item.getName());
            if (item.getPrice() > 0) {
                checkBox.setText(String.format("%s (+$%.2f)", item.getName(), item.getPrice()));
            }
            container.addView(checkBox);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                List<CustomizationSelection> selections = selectedCustomizations.getOrDefault(
                    option.getId(), new ArrayList<>());
                
                if (selections.isEmpty()) {
                    CustomizationSelection selection = new CustomizationSelection();
                    selection.setOptionId(option.getId());
                    selection.setOptionName(option.getName());
                    selection.setSelectedItems(new ArrayList<>());
                    selections.add(selection);
                }

                List<SelectedItem> selectedItems = selections.get(0).getSelectedItems();
                
                if (isChecked) {
                    SelectedItem selectedItem = new SelectedItem();
                    selectedItem.setId(item.getId());
                    selectedItem.setName(item.getName());
                    selectedItem.setPrice(item.getPrice());
                    selectedItems.add(selectedItem);
                } else {
                    selectedItems.removeIf(selected -> selected.getId().equals(item.getId()));
                }

                if (!selectedItems.isEmpty()) {
                    selectedCustomizations.put(option.getId(), selections);
                } else {
                    selectedCustomizations.remove(option.getId());
                }
                updateTotalPrice();
            });
        }
    }

    private void updateTotalPrice() {
        double customizationsTotal = calculateCustomizationsPrice();
        totalPrice = (menuItem.getPrice() + customizationsTotal) * quantity;
        totalPriceText.setText(String.format("$%.2f", totalPrice));
    }

    private double calculateCustomizationsPrice() {
        double total = 0;
        for (List<CustomizationSelection> selections : selectedCustomizations.values()) {
            for (CustomizationSelection selection : selections) {
                for (SelectedItem item : selection.getSelectedItems()) {
                    total += item.getPrice();
                }
            }
        }
        return total;
    }

    private CartItem createCartItem() {
        CartItem cartItem = new CartItem(menuItem);
        cartItem.setQuantity(quantity);
        cartItem.setCustomizations(selectedCustomizations);
        cartItem.setTotalPrice(totalPrice);
        return cartItem;
    }

    public interface OnAddToCartListener {
        void onAddToCart(CartItem cartItem);
    }
} 