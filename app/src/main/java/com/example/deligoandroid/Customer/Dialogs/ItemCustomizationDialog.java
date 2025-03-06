package com.example.deligoandroid.Customer.Dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private OnAddToCartListener listener;
    private int quantity = 1;
    private Map<String, Set<String>> selectedOptions = new HashMap<>();
    private double totalPrice;

    public static ItemCustomizationDialog newInstance(MenuItem menuItem) {
        ItemCustomizationDialog dialog = new ItemCustomizationDialog();
        Bundle args = new Bundle();
        args.putSerializable("menuItem", (Serializable) menuItem);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnAddToCartListener(OnAddToCartListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);
        menuItem = (MenuItem) getArguments().getSerializable("menuItem");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_item_customization, null);
        dialog.setContentView(view);

        setupViews(view);
        return dialog;
    }

    private void setupViews(View view) {
        // Setup item details
        ImageView itemImage = view.findViewById(R.id.itemImage);
        TextView itemName = view.findViewById(R.id.itemName);
        TextView itemDescription = view.findViewById(R.id.itemDescription);
        TextView itemPrice = view.findViewById(R.id.itemPrice);

        if (menuItem.getImageURL() != null && !menuItem.getImageURL().isEmpty()) {
            Glide.with(this)
                .load(menuItem.getImageURL())
                .centerCrop()
                .into(itemImage);
        }

        itemName.setText(menuItem.getName());
        itemDescription.setText(menuItem.getDescription());
        itemPrice.setText(String.format("$%.2f", menuItem.getPrice()));

        // Setup quantity selector
        TextView quantityText = view.findViewById(R.id.quantityText);
        ImageButton decreaseButton = view.findViewById(R.id.decreaseButton);
        ImageButton increaseButton = view.findViewById(R.id.increaseButton);

        decreaseButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                quantityText.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });

        increaseButton.setOnClickListener(v -> {
            quantity++;
            quantityText.setText(String.valueOf(quantity));
            updateTotalPrice();
        });

        // Setup customization options
        TextView customizationTitle = view.findViewById(R.id.customizationTitle);
        LinearLayout optionsContainer = view.findViewById(R.id.customizationContainer);
        
        if (menuItem.getCustomizationOptions() != null && !menuItem.getCustomizationOptions().isEmpty()) {
            customizationTitle.setVisibility(View.VISIBLE);
            
            for (CustomizationOption option : menuItem.getCustomizationOptions()) {
                View optionView = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_customization_option, optionsContainer, false);
                
                TextView optionName = optionView.findViewById(R.id.optionName);
                TextView requiredText = optionView.findViewById(R.id.requiredText);
                TextView selectionLimitText = optionView.findViewById(R.id.selectionLimitText);
                LinearLayout choicesContainer = optionView.findViewById(R.id.optionsContainer);

                optionName.setText(option.getName());
                if (option.isRequired()) {
                    requiredText.setVisibility(View.VISIBLE);
                }

                if (!option.isSingleSelection()) {
                    selectionLimitText.setVisibility(View.VISIBLE);
                    selectionLimitText.setText(String.format("Select up to %d", option.getMaxSelections()));
                }

                selectedOptions.put(option.getId(), new HashSet<>());

                for (CustomizationItem item : option.getOptions()) {
                    View choiceView = LayoutInflater.from(getContext()).inflate(
                        R.layout.item_customization_choice, choicesContainer, false);
                    
                    ImageView selectionIcon = choiceView.findViewById(R.id.selectionIcon);
                    TextView choiceName = choiceView.findViewById(R.id.choiceName);
                    TextView choicePrice = choiceView.findViewById(R.id.choicePrice);

                    selectionIcon.setImageResource(option.isSingleSelection() ? 
                        R.drawable.ic_radio_button : R.drawable.ic_checkbox);
                    choiceName.setText(item.getName());
                    if (item.getPrice() > 0) {
                        choicePrice.setText(String.format("+$%.2f", item.getPrice()));
                        choicePrice.setVisibility(View.VISIBLE);
                    } else {
                        choicePrice.setVisibility(View.GONE);
                    }

                    choiceView.setOnClickListener(v -> {
                        Set<String> selectedIds = selectedOptions.get(option.getId());
                        if (option.isSingleSelection()) {
                            // Single selection
                            selectedIds.clear();
                            selectedIds.add(item.getId());
                            updateChoicesUI(choicesContainer, option, item.getId());
                        } else {
                            // Multiple selection
                            if (selectedIds.contains(item.getId())) {
                                selectedIds.remove(item.getId());
                                selectionIcon.setImageResource(R.drawable.ic_checkbox);
                            } else if (selectedIds.size() < option.getMaxSelections()) {
                                selectedIds.add(item.getId());
                                selectionIcon.setImageResource(R.drawable.ic_checkbox_checked);
                            }
                        }
                        updateTotalPrice();
                    });

                    choicesContainer.addView(choiceView);
                }

                optionsContainer.addView(optionView);
            }
        } else {
            customizationTitle.setVisibility(View.GONE);
        }

        // Setup add to cart button
        Button addToCartButton = view.findViewById(R.id.addToCartButton);
        addToCartButton.setOnClickListener(v -> {
            if (isValid()) {
                addToCart();
                dismiss();
            }
        });

        updateTotalPrice();
    }

    private void updateChoicesUI(ViewGroup container, CustomizationOption option, String selectedId) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View choiceView = container.getChildAt(i);
            ImageView icon = choiceView.findViewById(R.id.selectionIcon);
            CustomizationItem item = option.getOptions().get(i);
            icon.setImageResource(item.getId().equals(selectedId) ? 
                R.drawable.ic_radio_button_checked : R.drawable.ic_radio_button);
        }
    }

    private void updateTotalPrice() {
        double basePrice = menuItem.getPrice();
        double customizationPrice = 0;

        // Calculate customization price
        for (Map.Entry<String, Set<String>> entry : selectedOptions.entrySet()) {
            CustomizationOption option = findOptionById(entry.getKey());
            if (option != null) {
                for (String selectedId : entry.getValue()) {
                    CustomizationItem item = findItemById(option, selectedId);
                    if (item != null) {
                        customizationPrice += item.getPrice();
                    }
                }
            }
        }

        totalPrice = (basePrice + customizationPrice) * quantity;
        TextView totalPriceView = getDialog().findViewById(R.id.totalPrice);
        totalPriceView.setText(String.format("$%.2f", totalPrice));

        // Update add to cart button state
        Button addToCartButton = getDialog().findViewById(R.id.addToCartButton);
        addToCartButton.setEnabled(isValid());
    }

    private boolean isValid() {
        if (menuItem.getCustomizationOptions() == null) return true;

        for (CustomizationOption option : menuItem.getCustomizationOptions()) {
            Set<String> selectedIds = selectedOptions.get(option.getId());
            if (option.isRequired() && (selectedIds == null || selectedIds.isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private void addToCart() {
        if (listener == null) return;

        CartItem cartItem = new CartItem();
        cartItem.setId(UUID.randomUUID().toString());
        cartItem.setMenuItemId(menuItem.getId());
        cartItem.setName(menuItem.getName());
        cartItem.setDescription(menuItem.getDescription());
        cartItem.setPrice(menuItem.getPrice());
        cartItem.setImageURL(menuItem.getImageURL());
        cartItem.setQuantity(quantity);
        cartItem.setTotalPrice(totalPrice);

        // Convert selections to customization format
        Map<String, List<CustomizationSelection>> customizations = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : selectedOptions.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            CustomizationOption option = findOptionById(entry.getKey());
            if (option == null) continue;

            List<SelectedItem> selectedItems = new ArrayList<>();
            for (String selectedId : entry.getValue()) {
                CustomizationItem item = findItemById(option, selectedId);
                if (item == null) continue;

                SelectedItem selectedItem = new SelectedItem();
                selectedItem.setId(item.getId());
                selectedItem.setName(item.getName());
                selectedItem.setPrice(item.getPrice());
                selectedItems.add(selectedItem);
            }

            CustomizationSelection selection = new CustomizationSelection();
            selection.setOptionId(option.getId());
            selection.setOptionName(option.getName());
            selection.setSelectedItems(selectedItems);
            customizations.put(option.getId(), Collections.singletonList(selection));
        }
        cartItem.setCustomizations(customizations);

        listener.onAddToCart(cartItem);
    }

    private CustomizationOption findOptionById(String optionId) {
        if (menuItem.getCustomizationOptions() == null) return null;
        for (CustomizationOption option : menuItem.getCustomizationOptions()) {
            if (option.getId().equals(optionId)) return option;
        }
        return null;
    }

    private CustomizationItem findItemById(CustomizationOption option, String itemId) {
        for (CustomizationItem item : option.getOptions()) {
            if (item.getId().equals(itemId)) return item;
        }
        return null;
    }

    public interface OnAddToCartListener {
        void onAddToCart(CartItem cartItem);
    }
} 