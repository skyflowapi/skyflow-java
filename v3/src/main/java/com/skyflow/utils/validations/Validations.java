package com.skyflow.utils.validations;

import com.skyflow.enums.InterfaceName;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.InsertRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Validations extends BaseValidations {
    private Validations() {
        super();
    }

    // add validations specific to v3 SDK
    public static void validateInsertRequest(InsertRequest insertRequest) throws SkyflowException {
        String table = insertRequest.getTable();
        ArrayList<HashMap<String, Object>> values = insertRequest.getValues();
        List<String> upsert = insertRequest.getUpsert();

        if (table == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
        } else if (table.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());
        } else if (values == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.VALUES_IS_REQUIRED.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ValuesKeyError.getMessage());
        } else if (values.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_VALUES.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValues.getMessage());
        } else if (upsert != null && upsert.isEmpty()){
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_UPSERT.getLog(), InterfaceName.INSERT.getName()
            ));
        }
        // upsert

        for (HashMap<String, Object> valuesMap : values) {
            for (String key : valuesMap.keySet()) {
                if (key == null || key.trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_KEY_IN_VALUES.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInValues.getMessage());
                } else {
                    Object value = valuesMap.get(key);
                    if (value == null || value.toString().trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_VALUE_IN_VALUES.getLog(),
                                InterfaceName.INSERT.getName(), key
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInValues.getMessage());
                    }
                }
            }
        }
    }

}
