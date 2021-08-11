package com.example.sendit_chatapp.packages;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.graphics.ColorSpace;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.sendit_chatapp.R;
import com.example.sendit_chatapp.databinding.ActivityChatDetailBinding;
import com.example.sendit_chatapp.packages.Adapters.ChatAdapter;
import com.example.sendit_chatapp.packages.Models.EncodeDecodeAES;
import com.example.sendit_chatapp.packages.Models.MessageModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ChatDetailActivity extends AppCompatActivity {
   ActivityChatDetailBinding binding;
   FirebaseDatabase database;
   FirebaseAuth auth;

   private Cipher cipher, decipher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }



        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        final String senderId = auth.getUid();
        String receiverId = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("userName");
        String profilePic = getIntent().getStringExtra("profilePic");
        binding.userName.setText(userName);
        Picasso.get().load(profilePic).placeholder(R.drawable.avatar).into(binding.profileImage2);

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatDetailActivity.this, MainActivity.class));
            }
        });

         ArrayList<MessageModel> messageModels = new ArrayList<>();
         ChatAdapter chatAdapter = new ChatAdapter(messageModels, this, receiverId);
        binding.chatsRecyclerView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatsRecyclerView.setLayoutManager(layoutManager);

        final String senderRoom = senderId+receiverId;
        final String receiverRoom = receiverId+senderId;



        database.getReference().child("chats")
                .child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {  //onDataChange
                        messageModels.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            MessageModel model = snapshot1.getValue(MessageModel.class);
                            model.setMessageId(snapshot1.getKey());

                            String decStr = null;
                            try {
                                decStr = AESDecryptionMethod(model.getMessage());
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            model.setMessage(decStr);

                            messageModels.add(model);

                    }
                        ChatAdapter chatAdapter = new ChatAdapter( messageModels, getApplicationContext());
                        binding.chatsRecyclerView.setAdapter(chatAdapter);

                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String message = (binding.etMessage.getText().toString());
                if (message.isEmpty()) {
                    binding.etMessage.setText("");
                } else if(message.trim().length() == 0){
                    binding.etMessage.setText("");
                }
                else {
                    String encMessage = null;
                    try {
                        encMessage = AESEncryptionMethod(message);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    MessageModel model = new MessageModel(senderId, encMessage);
                    model.setTimestamp(new Date().getTime());
                    binding.etMessage.setText("");
                    database.getReference().child("chats")
                            .child(senderRoom)
                            .push()
                            .setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            database.getReference().child("chats")
                                    .child(receiverRoom)
                                    .push()
                                    .setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private String AESEncryptionMethod(String string) throws UnsupportedEncodingException {
        EncodeDecodeAES aes = new EncodeDecodeAES();
        String encrypted = null;
        try {
            String entered = string;
            Toast.makeText(getApplicationContext() ,"the message before enc is"+entered,Toast.LENGTH_SHORT).show();

            encrypted = EncodeDecodeAES.bytesToHex(aes.encrypt(entered));
            Toast.makeText(getApplicationContext() ,"the message after enc is"+encrypted,Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } return encrypted;
    }

    public String AESDecryptionMethod(String string) throws UnsupportedEncodingException {
        EncodeDecodeAES aes = new EncodeDecodeAES();
        String decrypted = null;
        try {
            decrypted = new String(aes.decrypt(string));
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Toast.makeText(getApplicationContext() ,"the message after dec is"+decrypted,Toast.LENGTH_SHORT).show();
//
       return decrypted;
    }
}

//done till this point
//ab changes krunga