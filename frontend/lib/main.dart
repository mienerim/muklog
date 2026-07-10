import 'package:flutter/material.dart';

import 'screens/login_screen.dart';

void main() {
  runApp(const NomlogApp());
}

class NomlogApp extends StatelessWidget {
  const NomlogApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'nomlog',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
      ),
      home: const LoginScreen(),
    );
  }
}
