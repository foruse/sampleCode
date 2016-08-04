<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */

use common\components\db\Migration;

/**
 * Class m160415_113317_setup_language_table
 */
class m160415_113317_setup_language_table extends Migration
{
    /**
     * @inheritdoc
     */
    public function up()
    {
        $this->createTable(
            '{{%language}}',
            [
                'id' => $this->primaryKey()->unsigned(),
                'url' => $this->string(2)->notNull()->unique(),
                'locale' => $this->string(5)->notNull()->unique(),
                'name' => $this->string(125)->notNull(),
                'is_default' => $this->boolean()->notNull()->defaultValue(0),
            ],
            self::TABLE_OPTIONS
        );
    }

    /**
     * @inheritdoc
     */
    public function down()
    {
        $this->dropTable('{{%language}}');
    }
}
