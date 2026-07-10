import 'package:flutter_test/flutter_test.dart';

import 'package:nomlog/main.dart';

void main() {
  testWidgets('shows the login screen on launch', (WidgetTester tester) async {
    await tester.pumpWidget(const NomlogApp());

    expect(find.text('nomlog 로그인'), findsOneWidget);
  });
}
