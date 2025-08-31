package org.example.moliyaapp.utils;

public interface RestConstants {
    //***********GENERAL**************//
    String SUCCESS = "SUCCESS";
    String FAILED_TO_UPDATE = "Tahrirlashda xatolik yuz berdi..";
    String SUCCESSFULLY_DELETED = "Muvaffaqiyatli o'chirildi.";
    String SUCCESSFULLY_UPDATED = "Muvaffaqiyatli tahrirlandi.";
    String PASSWORD_SUCCESSFULLY_UPDATED = "Parol Muvaffaqiyatli tahrirlandi.";
    String SUCCESSFULLY_REGISTERED = "Muvaffaqiyatli ro'yxatdan o'tkazildi.";
    String SUCCESSFULLY_SAVED = "Muvaffaqiyatli saqlandi.";
    String INVALID_AMOUNT = "Kiritilgan miqdor toto'g'ri, iltimos tekshirib qaytadan kiriting.";
    String MISSING_DATA = "Kiritilgan ma'lumotlar yetarli emas, iltimos tekshirib qaytadan kiriting.";
    String INVALID_NUMBER = "Kiritilgan raqam toto'g'ri, iltimos tekshirib qaytadan kiriting.";
    String MONEY_IS_NOT_ENOUGH = "PUL MIQDORI YETARLI EMAS.";
    String PHONE_NUMBER_EXISTS = "TELEFON RAQAM ALLAQACHON MAVJUD.";
    String COMPANY_NOT_FOUND = "Tashkilot topilmadi.";
    String EMAIL_ALREADY_EXISTS = "BU EMAIL ALLAQACHON MAVJUD.";
    String USER_EMAIL_NOT_FOUND = "FOYDALANUVCHI E-POCHTASI (EMAIL) TOPILMADI.";


    //*****USER*******//
    String USER_NOT_FOUND = "Foydalanuvchi topilmadi.";
    String USER_ALREADY_EXIST = "Foydalanuvchi allaqachon mavjud.";
    String USER_DELETED = "Foydalanuvchi muvaffaqiyatli o'chirildi.";
    String USER_SAVED = "Foydalanuvchi muvaffaqiyatli saqlnadi.";
    String USER_UPDATED = "Foydalanuvchi muvaffaqiyatli tahrirlandi.";


    //*****TABEL*******//
    String TABEL_NOT_FOUND = "Tabel topilmadi.";
    String TABEL_ALREADY_EXIST = "Tabel allaqachon mavjud.";
    String TABEL_DELETED = "Tabel muvaffaqiyatli o'chirildi.";
    String TABEL_SAVED = "Tabel muvaffaqiyatli saqlnadi.";
    String TABEL_UPDATED = "Tabel muvaffaqiyatli tahrirlandi.";
    String TABLE_ALREADY_EXISTS_FOR_THIS_MONTH = "Bu oygi Tabel ushbu xodim uchun allaqachon to'ldirilgan.";


    //***********O'QITUVCHI********//
    String TEACHER_NOT_FOUND = "O'QITUVCHI TOPILMADI.";
    String TEACHER_ALREADY_EXIST = "O'qituvchi allaqachon mavjud.";
    String TEACHER_DELETED = "O'qituvchi muvaffaqiyatli o'chirildi.";
    String TEACHER_SAVED = "O'qituvchi muvaffaqiyatli saqlnadi.";
    String TEACHER_UPDATED = "O'qituvchi muvaffaqiyatli tahrirlandi.";
//**********O'QUVCHI*****//

    String STUDENT_NOT_FOUND = "O'quvchi topilmadi";
    String STUDENT_ALREADY_EXIST = "O'quvchi allaqachon mavjud.";
    String STUDENT_DELETED = "O'quvchi muvaffaqiyatli o'chirildi.";
    String STUDENT_SAVED = "O'quvchi muvaffaqiyatli saqlnadi.";
    String STUDENT_UPDATED = "O'quvchi muvaffaqiyatli tahrirlandi.";

    //****role///////
    String ROLE_NOT_FOUND = "Role topilmadi";
    String ROLE_ALREADY_EXISTS = "Role allaqachon mavjud.";
    String ROLE_ADDED_SUCCESSFULLY_TO_USER = "ROLE FOYDALANUVCHIGA MUVAFFAQIYATLI BERILDI.";
    String ROLE_DELETED = "Foydalanuvchi muvaffaqiyatli o'chirildi.";
    String ROLE_SAVED = "Foydalanuvchi muvaffaqiyatli saqlandi.";
    String ROLE_UPDATED = "Foydalanuvchi muvaffaqiyatli tahrirlandi.";
    String OWNER_ROLE_CANNOT_BE_CREATED = "Siz tashkilot rahbari 1 tadan ortiq qo'sha olmaysiz.";
    String OWNER_AND_PRINCIPAL_ROLE_CANNOT_BE_CREATED = "You cannot CREATE owner OR principal role!";
    String OWNER_ROLE_CANNOT_BE_DELETED = "You cannot DELETE owner role!";

    //******************SHARTNOMA**********//
    String CONTRACT_NOT_FOUND = "Shartnoma topilmadi.";
    String TRANSACTION_NOT_FOUND = "Transaksiya topilmadi.";
    String BONUS_TRANSACTION_NOT_FOUND = "Bonus Transaksiya topilmadi.";
    String CONTRACT_ALREADY_EXIST = "Shartnoma allaqachon mavjud.";
    String CONTRACT_DELETED = "Shartnoma muvaffaqiyatli o'chirildi.";
    String CONTRACT_SAVED = "Shartnoma muvaffaqiyatli saqlnadi.";
    String CONTRACT_UPDATED = "Shartnoma muvaffaqiyatli tahrirlandi.";
    String CONTRACT_TERMINATED = "Shartnoma muvaffaqiyatli to'xtatildi.";

    //******************XARAJAT**********//
    String EXPENSE_NOT_FOUND = "Xarajat topilmadi.";
    String EXPENSE_ALREADY_EXIST = "Xarajat allaqachon mavjud.";
    String EXPENSE_DELETED = "Xarajat muvaffaqiyatli o'chirildi.";
    String EXPENSE_SAVED = "Xarajat muvaffaqiyatli saqlnadi.";
    String EXPENSE_UPDATED = "Xarajat muvaffaqiyatli tahrirlandi.";

    //*********oylik to'lov**********//
    String MONTHLY_FEE_NOT_FOUND = "Oylik to'lov topilmadi";
    String MONTHLY_FEE_ALREADY_EXIST = "Oylik to'lov allaqachon mavjud.";
    String MONTHLY_FEE_DELETED = "Oylik to'lov muvaffaqiyatli o'chirildi.";
    String MONTHLY_FEE_SAVED = "Oylik to'lov muvaffaqiyatli saqlnadi.";
    String MONTHLY_FEE_UPDATED = "Oylik to'lov muvaffaqiyatli tahrirlandi.";

    //**************Kategory************//
    String CATEGORY_NOT_FOUND = "Kategoriya topilmadi.";
    String CATEGORY_ALREADY_EXIST = "Kategory allaqachon mavjud.";
    String CATEGORY_DELETED = "Kategory muvaffaqiyatli o'chirildi.";
    String CATEGORY_SAVED = "Kategory muvaffaqiyatli saqlnadi.";
    String CATEGORY_UPDATED = "Kategory muvaffaqiyatli tahrirlandi.";

    String BRANCH_NOT_FOUND = "FILIAL TOPILMADI.";
    String PARTNER_NOT_FOUND = "HAMKOR TOPILMADI";
    String TASK_NOT_FOUND = "MAZIFA TOPILMADI.";
    String SUBJECT_NOT_FOUND = "FAN TOPILMADI";
    String GROUP_NOT_FOUND = "Gruppa topilmadi.";
    String EMPLOYEE_BONUS_NOT_FOUND = "Bonus topilmadi.";
    String STUDENT_BONUS_NOT_FOUND = "Employee bonus not found";
    String SUBJECT_ALREADY_EXISTS = "FAN MAVJUD.";


    //    ****************Tariff**************
    String TARIFF_NOT_FOUND = "TARIFF TOPILMADI.";
    String TARIFF_DELETED = "TARIFF O'CHIRILDI.";
    String TARIFF_SAVED = "TARIFF QO'SHILDI.";
    String TARIFF_UPDATED = "TARIFF TAHRIRLANDI.";

    String SUCCESSFULLY_PAID = "Muvaffaqiyatli to'landi!";
    String FAILED_TO_PROCESS_REFUND = "XATOLIK.";
    String NOT_ENOUGH_CARD_BALANCE = "Kartada pul miqdori yetarli emas.";
    String NOT_ENOUGH_CASH_BALANCE = "Naqt pul miqdori yetarli emas.";
}
