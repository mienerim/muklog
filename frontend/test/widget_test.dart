import 'package:flutter_test/flutter_test.dart';

import 'package:muklog/main.dart';

void main() {
  testWidgets('shows the login screen on launch', (WidgetTester tester) async {
    await tester.pumpWidget(const MuklogApp());

    expect(find.text('muklog 로그인'), findsOneWidget);
  });
}
